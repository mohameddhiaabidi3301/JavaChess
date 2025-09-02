package engine;

public class TT {
	private static int TT_SIZE = 1 << 21;
	private static long[] TT_KEY32 = new long[TT_SIZE];
	private static int[] TT_MOVE32 = new int[TT_SIZE];
	private static int[] TT_SCORE32 = new int[TT_SIZE];
	private static byte[] TT_FLAG8 = new byte[TT_SIZE]; // First 2 bits: Cutoff
	private static byte[] TT_DEPTH8 = new byte[TT_SIZE];
	
	public static void set(long zobristHash, byte depth, int score, int pvMove, byte flag) {
		int index = (int)(zobristHash & (TT_SIZE - 1));
		
		if (TT_KEY32[index] == zobristHash) { // Overwrite?
			byte currentDepth = TT_DEPTH8[index];
			
			if (depth >= currentDepth) {
				TT_MOVE32[index] = pvMove;
				TT_SCORE32[index] = score;
				TT_FLAG8[index] = flag;
				TT_DEPTH8[index] = depth;
			}
		} else { // Create
			TT_KEY32[index] = zobristHash;
			TT_MOVE32[index] = pvMove;
			TT_SCORE32[index] = score;
			TT_FLAG8[index] = flag;
			TT_DEPTH8[index] = depth;
		}
	}
	
	public static int[] probe(long zobristHash, int alpha, int beta, byte depth) {
		int index = (int)(zobristHash & (TT_SIZE - 1));
		
		if (TT_KEY32[index] == zobristHash) {
			byte flagCutoff = (byte)(TT_FLAG8[index] & 0x3);
			int pvMove = TT_MOVE32[index];
			int score = TT_SCORE32[index];
			byte saveDepth = TT_DEPTH8[index];
			
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
