package com.bossbarskins.mixin;

import com.bossbarskins.SkinRuntime;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The "add this bar" packet: write the assigned colour and overlay instead of the requested ones. */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundBossEventPacket$AddOperation")
public abstract class AddOperationMixin {
    @Redirect(method = "<init>(Lnet/minecraft/world/BossEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;getColor()Lnet/minecraft/world/BossEvent$BossBarColor;"))
    private BossEvent.BossBarColor bossbarskins$color(BossEvent event) {
        return SkinRuntime.color(event);
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/BossEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;getOverlay()Lnet/minecraft/world/BossEvent$BossBarOverlay;"))
    private BossEvent.BossBarOverlay bossbarskins$overlay(BossEvent event) {
        return SkinRuntime.overlay(event);
    }
}
