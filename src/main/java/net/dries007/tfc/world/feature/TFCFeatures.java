/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.feature;

import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

public class TFCFeatures
{
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);

    public static final RegistryObject<CaveSpikesFeature> CAVE_SPIKES = FEATURES.register("cave_spikes", CaveSpikesFeature::new);
    public static final RegistryObject<LargeCaveSpikesFeature> LARGE_CAVE_SPIKES = FEATURES.register("large_cave_spikes", LargeCaveSpikesFeature::new);

    public static final RegistryObject<VeinsFeature> VEINS = FEATURES.register("veins", VeinsFeature::new);

    public static final RegistryObject<BouldersFeature> BOULDERS = FEATURES.register("boulders", BouldersFeature::new);
    public static final RegistryObject<FissureFeature> FISSURES = FEATURES.register("fissures", FissureFeature::new);
}