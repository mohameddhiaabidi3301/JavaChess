package main;
import engine.Position;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello World");
		
		Position.initOccupancy();
		System.out.println(Position.allOccupied);
	}
}
