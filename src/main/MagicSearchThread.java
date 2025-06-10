package main;
import engine.MagicBitboards;

public class MagicSearchThread extends Thread {
	int[] range;
	
	MagicSearchThread(int[] range) {
		this.range = range;
	};
	
	@Override
	public void run() {
		System.out.println("Thread running: " + Thread.currentThread().getName());
		MagicBitboards.calculateMagics(this.range);
	}
}
