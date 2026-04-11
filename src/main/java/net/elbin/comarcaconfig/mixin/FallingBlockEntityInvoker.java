package net.elbin.comarcaconfig.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityInvoker {

    // Este tag le dice a Mixin que queremos acceder al constructor (<init>)
    @Invoker("<init>")
    static FallingBlockEntity create(Level level, double x, double y, double z, BlockState state) {
        throw new AssertionError("El Mixin Invoker falló al generarse.");
    }
}