package com.example.nsga;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class Nsga2Service {

    private final ParcelRepository repo;
    public Nsga2Service(ParcelRepository repo) { this.repo = repo; }

    /* ======================= CẤU TRÚC CÁ THỂ ======================= */
    static class Ind {
        int[] genes;                 // K id lô (không trùng) — luôn được sort tăng dần để đại diện 1 tập
        double fRent, fNegPop, fComp;
        int rank; double crowd;
    }

    /* ======================= ĐIỂM VÀO CHÍNH ======================= */
    public Nsga2Result run(Nsga2Request req) {
        // --- Business defaults ---
        final int K      = (req.kLots != null && req.kLots > 0) ? req.kLots : 3;
        final int TOP    = (req.pickTopSets != null && req.pickTopSets > 0) ? req.pickTopSets : 5;
        final double rPop  = (req.populationRadius  != null) ? req.populationRadius  : 1000.0;
        final double rComp = (req.competitionRadius != null) ? req.competitionRadius : 500.0;
        final Double minDist = req.minDistanceMeters;
        final double distPenalty = (req.distancePenalty != null) ? req.distancePenalty : 0.0;
        final int LIMIT = (req.candidateLimit != null) ? req.candidateLimit : 200;

        final LocalDate cutoff = (req.availableFromIso != null && !req.availableFromIso.isBlank())
                ? LocalDate.parse(req.availableFromIso) : null;

        // --- Load data ---
        var feats = repo.fetchFeatures(LIMIT, rPop, rComp, req.maxRent, cutoff, req.whitelistCandidateIds);
        if (feats.size() < K) throw new IllegalStateException("Không đủ lô hợp lệ để chọn K = " + K);

        // Index nhanh
        Map<Integer, ParcelRepository.ParcelFeature> byId =
                feats.stream().collect(Collectors.toMap(f -> f.id, f -> f));
        List<Integer> universe = feats.stream().map(f -> f.id).toList();

        // Vị trí để tính khoảng cách phạt
        Map<Integer,double[]> pos = new HashMap<>();
        for (var f : feats) pos.put(f.id, new double[]{f.lat, f.lng});

        // --- Auto preset (ẩn cho người dùng) ---
        String preset = (req.preset == null) ? "balanced" : req.preset;
        int POP, GEN, TK;
        double PC, PM;

        int nCand = universe.size();
        int basePop = Math.min(300, Math.max(60, (int)(nCand * 1.0)));     // nội suy nhẹ theo dữ liệu
        int baseGen = Math.min(150, Math.max(40, (int)Math.ceil(nCand/2.0)));

        switch (preset) {
            case "fast"    -> { POP = (int)(basePop*0.6); GEN = (int)(baseGen*0.6); PC=0.9;  PM=0.25; TK=2; }
            case "quality" -> { POP = (int)(basePop*1.1); GEN = (int)(baseGen*1.2); PC=0.92; PM=0.18; TK=3; }
            default        -> { POP = basePop;            GEN = baseGen;            PC=0.90; PM=0.20; TK=2; }
        }
        // override nếu user mở Advanced
        if (req.populationSize != null) POP = req.populationSize;
        if (req.generations   != null) GEN = req.generations;
        if (req.crossoverProb != null) PC  = req.crossoverProb;
        if (req.mutationProb  != null) PM  = req.mutationProb;
        if (req.tournamentK   != null) TK  = req.tournamentK;
        POP = Math.max(40, Math.min(POP, 600));
        GEN = Math.max(30, Math.min(GEN, 300));

        // --- Khởi tạo quần thể ---
        List<Ind> population = new ArrayList<>(POP);
        for (int i=0;i<POP;i++){
            Ind z = new Ind();
            z.genes = sampleK(universe, K);
            evaluate(z, byId, pos, minDist, distPenalty);
            population.add(z);
        }

        // --- Tiến hóa NSGA-II ---
        for (int g=0; g<GEN; g++){
            var fronts = fastNonDominatedSort(population);
            fronts.forEach(this::crowdingDistance);

            List<Ind> offspring = new ArrayList<>(POP);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            while (offspring.size() < POP){
                Ind p1 = tournament(population, TK);
                Ind p2 = tournament(population, TK);
                Ind c1 = cloneInd(p1);
                Ind c2 = cloneInd(p2);

                if (rnd.nextDouble() < PC) crossoverSet(c1, c2, K);
                if (rnd.nextDouble() < PM) mutateSet(c1, universe, K);
                if (rnd.nextDouble() < PM) mutateSet(c2, universe, K);

                repair(c1, universe, K);
                repair(c2, universe, K);

                evaluate(c1, byId, pos, minDist, distPenalty);
                evaluate(c2, byId, pos, minDist, distPenalty);

                offspring.add(c1);
                if (offspring.size() < POP) offspring.add(c2);
            }

            // environmental selection
            List<Ind> union = new ArrayList<>(population.size()+offspring.size());
            union.addAll(population); union.addAll(offspring);
            var uFronts = fastNonDominatedSort(union);
            List<Ind> next = new ArrayList<>(POP);
            int f=0;
            while (f<uFronts.size() && next.size()+uFronts.get(f).size() <= POP){
                crowdingDistance(uFronts.get(f)); next.addAll(uFronts.get(f)); f++;
            }
            if (next.size()<POP && f<uFronts.size()){
                var last = uFronts.get(f); crowdingDistance(last);
                last.sort((a,b)->Double.compare(b.crowd,a.crowd));
                for (Ind z : last){ if (next.size()==POP) break; next.add(z); }
            }
            population = next;
        }

        // --- Kết quả & khuyến nghị ---
        var finals = fastNonDominatedSort(population);
        finals.forEach(this::crowdingDistance);

        List<Nsga2Result.SetSolution> all = new ArrayList<>();
        for (var fr : finals) for (var z : fr) all.add(toDto(z, byId));

        List<Nsga2Result.SetSolution> recs = List.of();
        if (!finals.isEmpty()){
            var f1 = finals.get(0);
            var best = topKByIdealDistance(f1, Math.max(1, TOP));
            // lọc bớt các phương án “gần giống”: khác tối thiểu 2 phần tử
            recs = diversify(best.stream().map(x->toDto(x, byId)).toList(), TOP);
        }

        Nsga2Result.Params p = new Nsga2Result.Params();
        p.kLots = K; p.populationSize=POP; p.generations=GEN;
        p.crossoverProb=PC; p.mutationProb=PM; p.tournamentK=TK;
        p.candidateCount=universe.size(); p.populationRadius=rPop; p.competitionRadius=rComp;
        p.minDistanceMeters=minDist; p.distancePenalty=distPenalty; p.pickTopSets=TOP; p.preset=preset;
        p.explain = String.format(
                "Đang đề xuất %d phương án, mỗi phương án gồm %d lô. " +
                        "Tối ưu đồng thời: Tổng chi phí ↓, Dân số phục vụ ↑, Cạnh tranh ↓. " +
                        "Chế độ '%s' (quần thể %d, thế hệ %d). Bán kính dân số %.0fm, cạnh tranh %.0fm%s.",
                Math.max(1, TOP), K, preset, POP, GEN, rPop, rComp,
                (minDist!=null? (", áp ràng buộc khoảng cách tối thiểu " + String.format("%.0f m", minDist)) : "")
        );

        Nsga2Result res = new Nsga2Result();
        res.paramsEcho = p;
        res.frontsFlat = all;
        res.recommendations = recs;
        return res;
    }

    /* ======================= EVALUATION (trên TẬP K) ======================= */
    private void evaluate(Ind ind,
                          Map<Integer, ParcelRepository.ParcelFeature> byId,
                          Map<Integer,double[]> posById,
                          Double minDist, double penalty){
        double sumRent=0, sumComp=0, sumPop=0, pen=0;
        for (int id : ind.genes){
            var f = byId.get(id);
            sumRent += f.rent;
            sumComp += f.compCnt;   // có thể thay bằng mật độ/trọng số theo khoảng cách
            sumPop  += f.popSum;    // xấp xỉ union-coverage
        }
        if (minDist != null && minDist > 0 && penalty > 0){
            for (int i=0;i<ind.genes.length;i++){
                for (int j=i+1;j<ind.genes.length;j++){
                    double d = haversineMeters(posById.get(ind.genes[i]), posById.get(ind.genes[j]));
                    if (d < minDist) pen += penalty * (minDist - d);
                }
            }
        }
        ind.fRent   = sumRent;
        ind.fComp   = sumComp + pen;
        ind.fNegPop = -sumPop;      // max dân số -> đổi dấu để min
    }

    /* ======================= K-ID OPERATORS ======================= */
    private int[] sampleK(List<Integer> universe, int K){
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int n = universe.size(); if (K > n) K = n;
        int[] idx = r.ints(0, n).distinct().limit(K).toArray();
        int[] g = new int[K]; for (int i=0;i<K;i++) g[i] = universe.get(idx[i]);
        Arrays.sort(g); return g;
    }

    /** Crossover: Set-based uniform + repair không trùng. */
    private void crossoverSet(Ind c1, Ind c2, int K){
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int[] a = c1.genes, b = c2.genes;
        boolean[] mask = new boolean[K];
        for (int i=0;i<K;i++) mask[i] = r.nextBoolean();

        int[] x = new int[K], y = new int[K]; Arrays.fill(x,-1); Arrays.fill(y,-1);
        Set<Integer> ux = new HashSet<>(), uy = new HashSet<>();
        int px=0, py=0;

        for (int i=0;i<K;i++){
            if (mask[i] && ux.add(a[i])) x[px++] = a[i];
            if (!mask[i] && uy.add(b[i])) y[py++] = b[i];
        }
        for (int v : b) if (px<K && ux.add(v)) x[px++] = v;
        for (int v : a) if (py<K && uy.add(v)) y[py++] = v;

        Arrays.sort(x); Arrays.sort(y);
        c1.genes = x; c2.genes = y;
    }

    /** Mutation: thay 1 gene bởi 1 id khác chưa có trong tập. */
    private void mutateSet(Ind c, List<Integer> universe, int K){
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (universe.size() <= K) return;
        int off = r.nextInt(K);
        Set<Integer> cur = Arrays.stream(c.genes).boxed().collect(Collectors.toSet());
        int ng;
        do { ng = universe.get(r.nextInt(universe.size())); } while (cur.contains(ng));
        c.genes[off] = ng; Arrays.sort(c.genes);
    }

    /** Repair: đảm bảo đúng K phần tử unique (hiếm khi cần nếu operator “sạch”). */
    private void repair(Ind c, List<Integer> universe, int K){
        Set<Integer> s = Arrays.stream(c.genes).boxed().collect(Collectors.toCollection(TreeSet::new));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        while (s.size() < K){
            int g = universe.get(r.nextInt(universe.size())); s.add(g);
        }
        while (s.size() > K){
            int i=r.nextInt(s.size()),k=0; Integer rem=null;
            for (Integer v: s){ if (k++==i){ rem=v; break; } }
            s.remove(rem);
        }
        c.genes = s.stream().mapToInt(Integer::intValue).toArray();
    }

    /* ======================= NSGA-II CORE ======================= */
    private boolean dominates(Ind a, Ind b){
        boolean ge = a.fRent<=b.fRent && a.fNegPop<=b.fNegPop && a.fComp<=b.fComp;
        boolean gt = a.fRent<b.fRent || a.fNegPop<b.fNegPop || a.fComp<b.fComp;
        return ge && gt;
    }

    private List<List<Ind>> fastNonDominatedSort(List<Ind> pop){
        List<List<Ind>> fronts = new ArrayList<>();
        Map<Ind,List<Ind>> S = new HashMap<>();
        Map<Ind,Integer> n = new HashMap<>();
        List<Ind> F1 = new ArrayList<>();
        for (Ind p : pop){
            S.put(p,new ArrayList<>()); n.put(p,0);
            for (Ind q : pop){
                if (dominates(p,q)) S.get(p).add(q);
                else if (dominates(q,p)) n.put(p, n.get(p)+1);
            }
            if (n.get(p)==0){ p.rank=1; F1.add(p); }
        }
        fronts.add(F1);
        int i=0;
        while (i<fronts.size()){
            List<Ind> Q = new ArrayList<>();
            for (Ind p : fronts.get(i)){
                for (Ind q : S.get(p)){
                    n.put(q, n.get(q)-1);
                    if (n.get(q)==0){ q.rank=i+2; Q.add(q); }
                }
            }
            if (Q.isEmpty()) break;
            fronts.add(Q); i++;
        }
        return fronts;
    }

    private void crowdingDistance(List<Ind> front){
        for (Ind z:front) z.crowd=0;
        if (front.size()<=2){ for (Ind z:front) z.crowd=Double.POSITIVE_INFINITY; return; }
        applyCrowding(front, Comparator.comparingDouble(a->a.fRent), z->z.fRent);
        applyCrowding(front, Comparator.comparingDouble(a->a.fNegPop), z->z.fNegPop);
        applyCrowding(front, Comparator.comparingDouble(a->a.fComp), z->z.fComp);
    }
    private interface G { double get(Ind z); }
    private void applyCrowding(List<Ind> front, Comparator<Ind> cmp, G g){
        front.sort(cmp);
        front.get(0).crowd=Double.POSITIVE_INFINITY;
        front.get(front.size()-1).crowd=Double.POSITIVE_INFINITY;
        double mn=g.get(front.get(0)), mx=g.get(front.get(front.size()-1)), den=Math.max(mx-mn,1e-12);
        for (int i=1;i<front.size()-1;i++){
            double nxt=g.get(front.get(i+1)), prv=g.get(front.get(i-1));
            front.get(i).crowd += (nxt-prv)/den;
        }
    }

    private Ind tournament(List<Ind> pop, int k){
        ThreadLocalRandom r=ThreadLocalRandom.current(); Ind best=null;
        for (int i=0;i<k;i++){ Ind c=pop.get(r.nextInt(pop.size())); if (best==null||better(c,best)) best=c; }
        return best;
    }
    private boolean better(Ind a, Ind b){ if (a.rank!=b.rank) return a.rank<b.rank; return Double.compare(a.crowd,b.crowd)>0; }

    private List<Ind> topKByIdealDistance(List<Ind> f1, int k){
        if (f1==null||f1.isEmpty()) return List.of();
        double minR=f1.stream().mapToDouble(z->z.fRent).min().orElse(0), maxR=f1.stream().mapToDouble(z->z.fRent).max().orElse(1);
        double minP=f1.stream().mapToDouble(z->z.fNegPop).min().orElse(0), maxP=f1.stream().mapToDouble(z->z.fNegPop).max().orElse(1);
        double minC=f1.stream().mapToDouble(z->z.fComp).min().orElse(0), maxC=f1.stream().mapToDouble(z->z.fComp).max().orElse(1);
        double rD=Math.max(maxR-minR,1e-12), pD=Math.max(maxP-minP,1e-12), cD=Math.max(maxC-minC,1e-12);
        return f1.stream().sorted((a,b)->{
            double da=dist((a.fRent-minR)/rD,(a.fNegPop-minP)/pD,(a.fComp-minC)/cD);
            double db=dist((b.fRent-minR)/rD,(b.fNegPop-minP)/pD,(b.fComp-minC)/cD);
            int cmp=Double.compare(da,db); if (cmp!=0) return cmp; return Double.compare(b.crowd,a.crowd);
        }).limit(k).toList();
    }
    private double dist(double x,double y,double z){ return Math.sqrt(x*x+y*y+z*z); }

    /* ======================= DTO & TIỆN ÍCH ======================= */
    private Nsga2Result.SetSolution toDto(Ind ind, Map<Integer, ParcelRepository.ParcelFeature> byId){
        Nsga2Result.SetSolution s = new Nsga2Result.SetSolution();
        s.objRent = ind.fRent; s.objNegPop = ind.fNegPop; s.objComp = ind.fComp;
        s.rank = ind.rank; s.crowdingDistance = ind.crowd;
        List<Integer> ids = Arrays.stream(ind.genes).boxed().toList();
        s.candidateIds = ids;
        List<Nsga2Result.Site> sites = new ArrayList<>();
        for (int id : ids){
            var f = byId.get(id);
            Nsga2Result.Site t = new Nsga2Result.Site();
            t.candidateId=id; t.name=f.name; t.address=f.address;
            t.lat=f.lat; t.lng=f.lng; t.rent=f.rent; t.pop1km=f.popSum; t.comp500m=f.compCnt;
            t.availableFrom = (f.availableFrom!=null) ? f.availableFrom.toString() : null;
            sites.add(t);
        }
        s.sites = sites;
        return s;
    }

    private List<Nsga2Result.SetSolution> diversify(List<Nsga2Result.SetSolution> sols, int top){
        List<Nsga2Result.SetSolution> out = new ArrayList<>();
        for (var s : sols){
            boolean tooSimilar = false;
            for (var e : out){
                if (overlap(s.candidateIds, e.candidateIds) >= Math.max(1, s.candidateIds.size()-1)){
                    tooSimilar = true; break;
                }
            }
            if (!tooSimilar){ out.add(s); if (out.size()==top) break; }
        }
        return out;
    }
    private static int overlap(List<Integer> a, List<Integer> b){
        var A = new ArrayList<>(a);
        var B=new ArrayList<>(b);
        Collections.sort(A); Collections.sort(B);
        int i=0,j=0,c=0;
        while(i<A.size() && j<B.size()){
            int x=A.get(i), y=B.get(j);
            if (x==y){ c++; i++; j++; }
            else if (x<y) i++; else j++;
        }
        return c;
    }

    private double haversineMeters(double[] a, double[] b){
        double R=6371000.0;
        double la1=Math.toRadians(a[0]), lo1=Math.toRadians(a[1]);
        double la2=Math.toRadians(b[0]), lo2=Math.toRadians(b[1]);
        double dLa=la2-la1, dLo=lo2-lo1;
        double h = Math.sin(dLa/2)*Math.sin(dLa/2) + Math.cos(la1)*Math.cos(la2)*Math.sin(dLo/2)*Math.sin(dLo/2);
        return 2*R*Math.asin(Math.sqrt(h));
    }

    private Ind cloneInd(Ind s){
        Ind t=new Ind();
        t.genes = Arrays.copyOf(s.genes, s.genes.length);
        t.fRent=s.fRent; t.fNegPop=s.fNegPop; t.fComp=s.fComp; t.rank=s.rank; t.crowd=s.crowd;
        return t;
    }
}