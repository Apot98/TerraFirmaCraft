/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.livestock.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Dynamic;
import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.EntityHelpers;
import net.dries007.tfc.common.entities.ai.pet.CatAi;
import net.dries007.tfc.common.entities.livestock.MammalProperties;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;

public class TFCCat extends TamableMammal
{
    public static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(TFCCat.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(TFCCat.class, EntityDataSerializers.INT);

    public TFCCat(EntityType<? extends TamableMammal> type, Level level)
    {
        super(type, level, TFCSounds.CAT, TFCConfig.SERVER.catConfig);
    }

    @Override
    public boolean willListenTo(Command command, boolean isClientSide)
    {
        if (!isClientSide && command == Command.SIT && getRandom().nextFloat() < 0.1f)
        {
            return false;
        }
        return super.willListenTo(command, isClientSide);
    }

    @Override
    public void createGenes(CompoundTag tag, TFCAnimalProperties male)
    {
        super.createGenes(tag, male);
        if (male instanceof TFCCat maleCat)
        {
            tag.putInt("catType", random.nextBoolean() ? maleCat.getCatType() : getCatType());
        }
    }

    @Override
    public void applyGenes(CompoundTag tag, MammalProperties baby)
    {
        super.applyGenes(tag, baby);
        if (baby instanceof TFCCat cat)
        {
            cat.setCatType(EntityHelpers.getIntOrDefault(tag, "catType", random.nextInt(10)));
        }
    }

    @Override
    public boolean canAttack(LivingEntity entity)
    {
        return super.canAttack(entity) && Helpers.isEntity(entity, TFCTags.Entities.HUNTED_BY_CATS);
    }

    @Override
    public void initCommonAnimalData()
    {
        super.initCommonAnimalData();
        setCatType(random.nextInt(10));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(DATA_TYPE, 0);
        entityData.define(DATA_COLLAR_COLOR, DyeColor.WHITE.getId());
    }

    public int getCatType()
    {
        return this.entityData.get(DATA_TYPE);
    }

    public void setCatType(int type)
    {
        if (type < 0 || type >= 11)
        {
            type = this.random.nextInt(10);
        }
        this.entityData.set(DATA_TYPE, type);
    }

    public DyeColor getCollarColor()
    {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public void setCollarColor(DyeColor color)
    {
        this.entityData.set(DATA_COLLAR_COLOR, color.getId());
    }

    public ResourceLocation getTextureLocation()
    {
        return Cat.TEXTURE_BY_TYPE.getOrDefault(getCatType(), Cat.TEXTURE_BY_TYPE.get(0));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("CatType", getCatType());
        tag.putInt("CollarColor", getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setCatType(tag.getInt("CatType"));
        setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
    }

    @Override
    protected Brain.Provider<? extends TamableMammal> brainProvider()
    {
        return Brain.provider(CatAi.MEMORY_TYPES, CatAi.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic)
    {
        return CatAi.makeBrain(brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<? extends TamableMammal> getBrain()
    {
        return (Brain<TamableMammal>) super.getBrain();
    }

    @SuppressWarnings("unchecked")
    public void tickBrain()
    {
        ((Brain<TamableMammal>) getBrain()).tick((ServerLevel) level, this);
        CatAi.updateActivity(this, tickCount % 20 == 0);
    }

    @Override
    public TagKey<Item> getFoodTag()
    {
        return TFCTags.Items.CAT_FOOD;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size)
    {
        return size.height * 0.5F;
    }

    @Override
    public void receiveCommand(ServerPlayer player, Command command)
    {
        if (getOwner() != null && getOwner().equals(player))
        {
            playSound(SoundEvents.CAT_PURREOW, getSoundVolume(), getVoicePitch());
        }
        super.receiveCommand(player, command);
    }
}
