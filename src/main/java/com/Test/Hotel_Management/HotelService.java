package com.Test.Hotel_Management;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HotelService {

    public static final int FLOORS = 10;
    public static final int MAX_PER_BOOKING = 5;

    public record RoomView(int number, String status) {}
    public record FloorView(int floor, List<RoomView> rooms) {}
    public record State(List<FloorView> floors, int available, int occupied) {}
    public record BookingResult(List<Integer> rooms, int travelTime, String message) {}

    private final Set<Integer> occupied = new HashSet<>();
    private Set<Integer> lastBooked = new TreeSet<>();
    private final Random random = new Random();

    // ---------- Building model ----------
    static int roomsOn(int floor) { return floor == FLOORS ? 7 : 10; }
    static int floorOf(int room)  { return room / 100; }
    static int posOf(int room)    { return room % 100; }   // 1 = closest to stairs/lift

    //Travel time in minutes between two rooms
    static int travel(int a, int b) {
        int fa = floorOf(a), fb = floorOf(b), pa = posOf(a), pb = posOf(b);
        if (fa == fb) return Math.abs(pa - pb);
        return (pa - 1) + (pb - 1) + 2 * Math.abs(fa - fb);
    }

    /** Longest travel time between any two rooms of the set (first-to-last room). */
    static int diameter(List<Integer> rooms) {
        int max = 0;
        for (int i = 0; i < rooms.size(); i++)
            for (int j = i + 1; j < rooms.size(); j++)
                max = Math.max(max, travel(rooms.get(i), rooms.get(j)));
        return max;
    }

    static int sumOfPairs(List<Integer> rooms) {
        int sum = 0;
        for (int i = 0; i < rooms.size(); i++)
            for (int j = i + 1; j < rooms.size(); j++)
                sum += travel(rooms.get(i), rooms.get(j));
        return sum;
    }

    /** Minutes from the stairs/lift on floor 1 to this room. */
    static int fromStairs(int room) {
        return (posOf(room) - 1) + 2 * (floorOf(room) - 1);
    }

    static int totalFromStairs(List<Integer> rooms) {
        return rooms.stream().mapToInt(HotelService::fromStairs).sum();
    }

    /**
     * 1) max travel time between rooms, 2) total pairwise travel time,
     * 3) closeness to the stairs/lift (so ties pick the nearest rooms, e.g. 101 for a single room).
     */
    static int cost(List<Integer> rooms) {
        return diameter(rooms) * 1_000_000 + sumOfPairs(rooms) * 1_000 + totalFromStairs(rooms);
    }

    // ---------- Operations ----------
    public synchronized BookingResult book(int count) {
        if (count < 1 || count > MAX_PER_BOOKING)
            throw new IllegalArgumentException("You can book between 1 and " + MAX_PER_BOOKING + " rooms at a time.");

        List<Integer> free = freeRooms();
        if (free.size() < count)
            throw new IllegalStateException("Only " + free.size() + " room(s) are available.");

        List<Integer> best = null;
        int bestCost = Integer.MAX_VALUE;

        // Rule 1 & 2: same floor first -> tightest window of free rooms on a single floor.
        for (int f = 1; f <= FLOORS; f++) {
            final int floor = f;
            List<Integer> onFloor = free.stream().filter(r -> floorOf(r) == floor).toList();
            for (int i = 0; i + count <= onFloor.size(); i++) {
                List<Integer> cand = onFloor.subList(i, i + count);
                int c = cost(cand);
                if (c < bestCost) { bestCost = c; best = new ArrayList<>(cand); }
            }
            // Lower floors win ties because they are visited first (strict <).
        }

        // Rule 3 & 4: no single floor is enough -> span floors, minimising vertical + horizontal travel.
        if (best == null) {
            for (int lo = 1; lo <= FLOORS; lo++) {
                for (int hi = lo + 1; hi <= FLOORS; hi++) {
                    final int l = lo, h = hi;
                    List<Integer> pool = free.stream()
                            .filter(r -> floorOf(r) >= l && floorOf(r) <= h)
                            .sorted(Comparator.comparingInt(HotelService::posOf)
                                    .thenComparingInt(HotelService::floorOf))
                            .toList();
                    if (pool.size() < count) continue;
                    List<Integer> cand = new ArrayList<>(pool.subList(0, count));
                    int c = cost(cand);
                    if (c < bestCost) { bestCost = c; best = cand; }
                }
            }
        }

        Collections.sort(best);
        occupied.addAll(best);
        lastBooked = new TreeSet<>(best);
        int time = diameter(best);
        String msg = best.size() == 1
                ? "Booked room " + best.get(0) + ". It is " + fromStairs(best.get(0)) + " min from the stairs/lift."
                : "Booked " + best.size() + " rooms. Travel time between first and last room: " + time + " min.";
        return new BookingResult(best, time, msg);
    }

    public synchronized State randomOccupancy() {
        occupied.clear();
        lastBooked = new TreeSet<>();
        for (int f = 1; f <= FLOORS; f++)
            for (int p = 1; p <= roomsOn(f); p++)
                if (random.nextDouble() < 0.4) occupied.add(f * 100 + p);
        return state();
    }

    public synchronized State reset() {
        occupied.clear();
        lastBooked = new TreeSet<>();
        return state();
    }

    public synchronized State state() {
        List<FloorView> floors = new ArrayList<>();
        for (int f = FLOORS; f >= 1; f--) {           // top floor first
            List<RoomView> rooms = new ArrayList<>();
            for (int p = 1; p <= roomsOn(f); p++) {
                int n = f * 100 + p;
                String status = lastBooked.contains(n) ? "booked" : occupied.contains(n) ? "occupied" : "available";
                rooms.add(new RoomView(n, status));
            }
            floors.add(new FloorView(f, rooms));
        }
        return new State(floors, freeRooms().size(), occupied.size());
    }

    private List<Integer> freeRooms() {
        List<Integer> free = new ArrayList<>();
        for (int f = 1; f <= FLOORS; f++)
            for (int p = 1; p <= roomsOn(f); p++)
                if (!occupied.contains(f * 100 + p)) free.add(f * 100 + p);
        return free;
    }
}