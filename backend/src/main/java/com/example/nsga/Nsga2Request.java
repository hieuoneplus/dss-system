package com.example.nsga;

import java.util.List;

public class Nsga2Request {
    public java.util.List<Integer> cityIds;
    public Integer kLots;                 // Số lô cần chọn trong 1 phương án (K)
    public Integer pickTopSets;           // Số phương án muốn đề xuất (Top-N)

    public Double maxRent;                // Giá thuê tối đa (lọc)
    public String availableFromIso;       // Lô sẵn có từ ngày... (YYYY-MM-DD)

    public Double populationRadius;       // m, mặc định 1000
    public Double competitionRadius;      // m, mặc định 500

    public Double minDistanceMeters;      // m, optional - tránh 2 lô quá gần nhau
    public Double distancePenalty;        // 0..1 - mức phạt khi < minDistance (optional)

    public List<Integer> whitelistCandidateIds; // optional
    public Integer candidateLimit;        // mặc định 200

    // ---- Advanced (ẩn trong UI, không bắt buộc) ----
    public String preset;                 // "fast" | "balanced" | "quality" (mặc định balanced)
    public Integer populationSize;        // override
    public Integer generations;           // override
    public Double crossoverProb;          // override
    public Double mutationProb;           // override
    public Integer tournamentK;           // override
}
