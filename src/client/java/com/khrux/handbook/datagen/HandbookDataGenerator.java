package com.khrux.handbook.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class HandbookDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(final FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(HandbookModelProvider::new);
		pack.addProvider(HandbookLanguageProvider::new);
	}
}
