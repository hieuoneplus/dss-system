export interface Nsga2Request {
  cityIds?: number[];
  kLots?: number;                // K
  pickTopSets?: number;          // Top-N

  maxRent?: number;
  availableFromIso?: string;     // 'YYYY-MM-DD'

  populationRadius?: number;     // m
  competitionRadius?: number;    // m

  minDistanceMeters?: number;    // optional
  distancePenalty?: number;      // 0..1 optional

  whitelistCandidateIds?: number[];
  candidateLimit?: number;

  // Advanced (ẩn)
  preset?: 'fast' | 'balanced' | 'quality';
  populationSize?: number;
  generations?: number;
  crossoverProb?: number;
  mutationProb?: number;
  tournamentK?: number;
}
export interface NsgaIndividual {
  candidateId: number;
  name: string;
  address: string;
  lat: number;
  lng: number;
  objRent: number;
  objNegPop: number;
  objComp: number;
  rent: number;
  pop1km: number;
  comp500m: number;
  rank: number;
  crowdingDistance: number;
  availableFrom?: string;
}
export interface Nsga2Result {
  paramsEcho: {
    kLots: number;
    populationSize: number;
    generations: number;
    crossoverProb: number;
    mutationProb: number;
    tournamentK: number;
    candidateCount: number;
    populationRadius: number;
    competitionRadius: number;
    minDistanceMeters?: number;
    distancePenalty?: number;
    pickTopSets: number;
    preset?: string;
    explain: string;
  };
  frontsFlat: SetSolution[];
  recommendations: SetSolution[];
}

export interface Site {
  candidateId: number;
  name: string;
  address: string;
  lat: number;
  lng: number;
  rent: number;
  pop1km: number;
  comp500m: number;
  availableFrom?: string;
}

export interface SetSolution {
  sites: Site[];
  candidateIds: number[];
  objRent: number;        // min
  objNegPop: number;      // min (-population)
  objComp: number;        // min
  rank: number;
  crowdingDistance: number;
}

