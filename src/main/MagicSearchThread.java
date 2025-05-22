package main;
import engine.MagicBitboards;

public class MagicSearchThread extends Thread {
	int rangeMin;
	int rangeMax;
	
	MagicSearchThread(int rangeMin, int rangeMax) {
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	};
	
	@Override
	public void run() {
		System.out.println("Thread running: " + Thread.currentThread().getName());
		MagicBitboards.calculateMagics(this.rangeMin, this.rangeMax);
	}
}
