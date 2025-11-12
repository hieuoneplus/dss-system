package com.example.nsga;

import java.util.List;

public class Nsga2Result {

    public static class Params {
        public int kLots;
        public int populationSize;
        public int generations;
        public double crossoverProb;
        public double mutationProb;
        public int tournamentK;
        public int candidateCount;
        public Double populationRadius;
        public Double competitionRadius;
        public Double minDistanceMeters;
        public Double distancePenalty;
        public Integer pickTopSets;
        public String preset;
        public String explain;   // câu diễn giải dễ hiểu cho người dùng
    }

    public static class Site {
        public int candidateId;
        public String name;
        public String address;
        public double lat, lng;
        public double rent;      // VND/tháng
        public double pop1km;    // dân số trong populationRadius (repo đã tính)
        public double comp500m;  // số đối thủ trong competitionRadius (repo đã tính)
        public String availableFrom;
    }

    /** Một phương án = tập K lô */
    public static class SetSolution {
        public List<Site> sites;          // K lô
        public List<Integer> candidateIds; // chỉ số lô
        // 3 mục tiêu (min): tổng chi phí, -tổng dân số (để min), tổng cạnh tranh
        public double objRent;
        public double objNegPop;
        public double objComp;
        public int rank;
        public double crowdingDistance;
    }

    public Params paramsEcho;
    public List<SetSolution> frontsFlat;       // tất cả front (flatten), giữ rank/crowding
    public List<SetSolution> recommendations;  // Top-N từ Front 1
}
