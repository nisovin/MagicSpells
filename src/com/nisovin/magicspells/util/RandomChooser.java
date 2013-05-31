package com.nisovin.magicspells.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RandomChooser<T> {

	Random random;
	Map<T, Double> options;
	
	public RandomChooser() {
		random = new Random();
		options = new LinkedHashMap<T, Double>();
	}
	
	public RandomChooser<T> option(T object, double percentChance) {
		options.put(object, percentChance);
		return this;
	}
	
	public T choose() {
		double d = 0;
		double rand = random.nextDouble() * 100;
		System.out.println("rand: " +  rand);
		
		for (T obj : options.keySet()) {
			double pct = options.get(obj);
			if (rand < pct + d) {
				return obj;
			} else {
				d += pct;
			}
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		RandomChooser<String> chooser = new RandomChooser<String>();
		chooser.option("a", 2);
		chooser.option("b", 8);
		chooser.option("c", 5);
		chooser.option("d", 10);
		String randomChoice = chooser.choose();
		System.out.println(randomChoice);
	}
	
}
