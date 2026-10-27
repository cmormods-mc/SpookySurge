package com.bossbarskins.mixin;

import com.bossbarskins.SkinRuntime;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The "style changed" packet (a mod called setColor or setOverlay on a live bar). */
@Mixin(ClientboundBossEventPacket.class)
public abstract class BossEventPacketMixin {
    @Redirect(method = "createUpdateStylePacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;getColor()Lnet/minecraft/world/BossEvent$BossBarColor;"))
    private static BossEvent.BossBarColor bossbarskins$color(BossEvent event) {
        return SkinRuntime.color(event);
    }

    @Redirect(method = "createUpdateStylePacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;getOverlay()Lnet/minecraft/world/BossEvent$BossBarOverlay;"))
    private static BossEvent.BossBarOverlay bossbarskins$overlay(BossEvent event) {
        return SkinRuntime.overlay(event);
    }
}
