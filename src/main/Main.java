package main;

import engine.Position;
import engine.LegalityCheck;
import engine.MoveGen;
import engine.PrecompMoves;
import engine.MagicBitboards;
import engine.Minimax;

import java.util.Arrays;

import debug.DebugRender;
import gui.Board;

import javax.swing.SwingUtilities;

/*
 improved Main launcher:
  basic CLI options (--fen, --autoplay, --depth, --timeMs, --moves)
  initialization timing/logging
  autoplay runs in a background thread to keep GUI responsive
  basic error handling
 */
public class Main {
    public static volatile Position globalPosition;

    public static void main(String[] args) {
        System.out.println("=== JavaChess starting ===");
        final long t0 = System.nanoTime();

        LaunchOptions opts = LaunchOptions.fromArgs(args);
        if (opts.showHelp) {
            LaunchOptions.printUsage();
            return;
        }

        try {
            System.out.println("Initializing engine subsystems...");
            long tInitStart = System.nanoTime();
            LegalityCheck.init();
            MagicBitboards.initBishopLookups();
            MagicBitboards.initRookLookups();
            MagicBitboards.initPrecomputedLineBB();
            MagicBitboards.initMagicMasks();
            Position.initGlobalZobristKeys(); // Zobrist Keys are global across all threads
            long tInit = System.nanoTime() - tInitStart;
            System.out.printf("Initialization complete (%.2f ms)%n", tInit / 1_000_000.0);
        } catch (Throwable e) {
            System.err.println("Fatal error during initialization: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        final String startFen = opts.fen != null ? opts.fen :
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq";
        Position pos;
        try {
            pos = new Position(startFen);
        } catch (Throwable e) {
            System.err.println("Failed to create Position from FEN. Using default start position.");
            pos = new Position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq");
        }
        globalPosition = pos;

        try {
            SwingUtilities.invokeAndWait(() -> {
                Board.init();
                Board.renderAllPieces();
            });
        } catch (Exception e) {
            System.err.println("Warning: GUI init on EDT failed, trying direct init.");
            Board.init();
            Board.renderAllPieces();
        }

        long tAttackStart = System.nanoTime();
        try {
            globalPosition.updateAttacksTEST((byte)0);
            globalPosition.updateAttacksTEST((byte)1);
        } catch (Throwable e) {
            System.err.println("Warning: updateAttacksTEST error: " + e.getMessage());
        }
        System.out.printf("Attack maps updated (%.2f ms)%n", (System.nanoTime() - tAttackStart) / 1_000_000.0);

        System.out.printf("Startup total: %.2f ms%n", (System.nanoTime() - t0) / 1_000_000.0);
        if (opts.autoplay) {
            System.out.printf("Starting autoplay: depth=%d, timeMs=%d, moves=%d%n",
                    opts.depth, opts.timeMs, opts.maxMoves);
            Thread autoplay = new Thread(() -> runAutoplay(opts), "autoplay-thread");
            autoplay.setDaemon(true);
            autoplay.start();
        }
    }

    private static void runAutoplay(LaunchOptions opts) {
        int moves = 0;
        while (opts.maxMoves <= 0 || moves < opts.maxMoves) {
            try {
                int move = Minimax.getComputerMove(opts.depth, opts.timeMs);

                if (move == 0) {
                    System.out.println("Engine returned move 0 — stopping autoplay.");
                    break;
                }

                synchronized (Main.class) {
                    Main.globalPosition.makeMove(move, false);
                }

                try {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Board.renderAllPieces();
                        } catch (Throwable t) {
                            System.err.println("Render error: " + t.getMessage());
                        }
                    });
                } catch (Throwable t) {
                    try { Board.renderAllPieces(); } catch (Throwable ex) { /* ignore */ }
                }

                moves++;
                System.out.printf("Autoplay: move #%d played (move=%d)%n", moves, move);

                Thread.sleep(Math.max(100, opts.timeMs));
            } catch (InterruptedException ie) {
                System.out.println("Autoplay interrupted, exiting.");
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                System.err.println("Autoplay error: " + t.getMessage());
                t.printStackTrace();
                break;
            }
        }
        System.out.println("Autoplay finished.");
    }

    private static class LaunchOptions {
        String fen = null;
        boolean autoplay = false;
        int depth = 1;
        int timeMs = 500;
        int maxMoves = 0; 
        boolean showHelp = false;

        static LaunchOptions fromArgs(String[] args) {
            LaunchOptions o = new LaunchOptions();
            for (String a : args) {
                if (a == null) continue;
                if (a.equals("--help") || a.equals("-h")) {
                    o.showHelp = true;
                } else if (a.startsWith("--fen=")) {
                    o.fen = a.substring("--fen=".length());
                } else if (a.equals("--autoplay")) {
                    o.autoplay = true;
                } else if (a.startsWith("--depth=")) {
                    try { o.depth = Integer.parseInt(a.substring("--depth=".length())); } catch (NumberFormatException ignored) {}
                } else if (a.startsWith("--timeMs=")) {
                    try { o.timeMs = Integer.parseInt(a.substring("--timeMs=".length())); } catch (NumberFormatException ignored) {}
                } else if (a.startsWith("--moves=")) {
                    try { o.maxMoves = Integer.parseInt(a.substring("--moves=".length())); } catch (NumberFormatException ignored) {}
                } else {
                    System.out.println("Unknown arg: " + a);
                }
            }
            return o;
        }

        static void printUsage() {
            System.out.println("Usage: java -cp <classpath> main.Main [options]");
            System.out.println("Options:");
            System.out.println("  --fen=<FEN>         Start from given FEN");
            System.out.println("  --autoplay          Let AI play automatically (AI vs AI)");
            System.out.println("  --depth=<n>         AI search depth (default 1)");
            System.out.println("  --timeMs=<ms>       Time budget passed to AI (default 500)");
            System.out.println("  --moves=<n>         Stop after n moves (0 = unlimited)");
            System.out.println("  --help, -h          Show this help");
        }
    }
}
