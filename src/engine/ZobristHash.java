package engine;
import java.util.Random;
public class ZobristHash {
	// Indexes matches the ones used in positions | bitboards
	private static long[][] zobristKeys = new long[13][];
	private static long[] enPassantKeys = new long[64];
	private static long[] castlingKeys = new long[4];
	private static long whiteToMoveKey;
	
	public static long hash = 0L;
	
	public static void initZobrist() {
		Random rng = new Random();
		
		// Pieces
		for (int i = 0; i < 13; i++) {
			zobristKeys[i] = new long[64];
			for (int j = 0; j < 64; j++) {
				zobristKeys[i][j] = rng.nextLong();
				enPassantKeys[j] = rng.nextLong();
			}
		}
		
		// Side to move
		whiteToMoveKey = rng.nextLong();
		
		// Castling
		for (int i = 0; i < 4; i++) {
			castlingKeys[i] = rng.nextLong();
			
			if (Position.castlingRights[i]) {
				hash ^= castlingKeys[i];
			}
		}
		
		// Initializing Hash
		for (int square = 0; square < 64; square++) {
			int pieceType = Position.engineLookup[square];
			long hashValue = zobristKeys[pieceType][square];
			
			hash ^= hashValue;
		}
		
		if (Position.sideToMove == 0) {hash ^= whiteToMoveKey;};
		if (Position.enPassantTarget != -1) {hash ^= enPassantKeys[Position.enPassantTarget];};
	}
	
	public static void updateZobrist(int move) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte targetKey = (byte)((move >>> 16) & 0xF);
		byte promotionKey = (byte)((move >>> 22) & 0xF);
		
		byte color = (byte)((move >>> 31) & 1L);
		boolean isCapture = targetKey != 0;
		boolean isDoublePawn = ((byte)((move >>> 30) & 1L)) != 0;
		boolean isEnPassant = ((byte)(move >>> 29) & 1L) != 0;
		boolean isCastle = ((byte)(move >>> 28) & 1L) != 0;
		byte castleType = (byte)((move >>> 20) & 3L);
		
		hash ^= zobristKeys[originKey][from];
		hash ^= zobristKeys[originKey][to];
		
		if (isCapture) {hash ^= zobristKeys[targetKey][to];};
		hash ^= whiteToMoveKey;
		
		if (isEnPassant) {hash ^= enPassantKeys[to];};
		
		for (byte i = 0; i < 4; i++) {
			if (Position.castlingRights[i]) {
				hash ^= castlingKeys[i];
			}
		}
		
		if (isCastle) {
			boolean isCastleShort = castleType % 2 == 0;
			byte rookType = (byte)((color == 0) ? 4 : 10);
			if (isCastleShort) {
				hash ^= zobristKeys[rookType][from + 3];
				hash ^= zobristKeys[rookType][from + 1];
			} else {
				hash ^= zobristKeys[rookType][from - 4];
				hash ^= zobristKeys[rookType][from - 1];
			}
		}
		
		if (promotionKey != 0) {
			hash ^= zobristKeys[originKey][to];
			hash ^= zobristKeys[promotionKey][to];
		}
		
		
		//-4002584257597694895
		
		System.out.println(hash);
	}
}