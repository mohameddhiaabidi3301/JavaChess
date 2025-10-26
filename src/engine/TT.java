package engine;

public class TT {
	private static int TT_SIZE = 1 << 23;
	private static long[] TT_KEY64 = new long[TT_SIZE];
	private static short[] TT_VERIFY16 = new short[TT_SIZE];
	private static int[] TT_MOVE32 = new int[TT_SIZE];
	private static int[] TT_SCORE32 = new int[TT_SIZE];
	private static byte[] TT_FLAG8 = new byte[TT_SIZE]; // First 2 bits: Cutoff
	private static byte[] TT_DEPTH8 = new byte[TT_SIZE];
	private static byte[] TT_AGE8 = new byte[TT_SIZE];
	
	private static byte globalAge = 0;
	public static void nextRootSearch() {
		globalAge = (byte)((globalAge + 1) & 0xFF);
	}
 	
	public static void set(long zobristHash, byte depth, int score, int pvMove, byte flag, boolean forceOverwrite) {
		int index = (int)(zobristHash & (TT_SIZE - 1));
		
		if (TT_KEY64[index] == zobristHash && TT_VERIFY16[index] == (short)(zobristHash >>> 48)) { // Overwrite?
			byte currentDepth = TT_DEPTH8[index];
			
			if (forceOverwrite || (depth >= currentDepth || TT_AGE8[index] != globalAge)) {
				TT_MOVE32[index] = pvMove;
				TT_SCORE32[index] = score;
				TT_FLAG8[index] = flag;
				TT_DEPTH8[index] = depth;
				TT_AGE8[index] = globalAge;
			}
		} else { // Create
			TT_KEY64[index] = zobristHash;
			TT_VERIFY16[index] = (short)(zobristHash >>> 48);
			TT_MOVE32[index] = pvMove;
			TT_SCORE32[index] = score;
			TT_FLAG8[index] = flag;
			TT_DEPTH8[index] = depth;
			TT_AGE8[index] = globalAge;
		}
	}
	
	public static int[] probe(long zobristHash, int alpha, int beta, byte depth, int ply) {
		int index = (int)(zobristHash & (TT_SIZE - 1));
		
		if (TT_KEY64[index] == zobristHash && TT_VERIFY16[index] == (short)(zobristHash >>> 48)) {
			byte flagCutoff = (byte)(TT_FLAG8[index] & 0x3);
			int pvMove = TT_MOVE32[index];
			int score = TT_SCORE32[index];
			byte saveDepth = TT_DEPTH8[index];
			
			if (score >= Minimax.MATE_SCORE) {
				score = Minimax.MATE_SCORE + ply;
			}
			
			if (saveDepth < depth) return null;
			
			if (flagCutoff == Minimax.FLAG_LOWERBOUND && score >= beta) {
				return new int[] {score, pvMove};
			} else if (flagCutoff == Minimax.FLAG_UPPERBOUND && score <= alpha) {
				return new int[] {score, pvMove};
			} else if (flagCutoff == Minimax.FLAG_EXACT) {
				return new int[] {score, pvMove};
			}
		}
		
		return null;
	}
}
