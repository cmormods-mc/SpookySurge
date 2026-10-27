package com.bossbarskins.mixin;

import com.bossbarskins.OwnerResolver;
import com.bossbarskins.api.OwnerHolder;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers which mod's code constructed each boss event. */
@Mixin(BossEvent.class)
public abstract class BossEventMixin implements OwnerHolder {
    @Unique private String bossbarskins$owner;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bossbarskins$captureOwner(UUID id, Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, CallbackInfo ci) {
        this.bossbarskins$owner = OwnerResolver.currentOwner();
    }

    @Override
    public String bossbarskins$getOwner() {
        return bossbarskins$owner;
    }

    @Override
    public void bossbarskins$setOwner(String modId) {
        this.bossbarskins$owner = modId;
    }
}
