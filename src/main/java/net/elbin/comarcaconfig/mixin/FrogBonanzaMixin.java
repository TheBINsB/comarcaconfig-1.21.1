package net.elbin.comarcaconfig.mixin;

import dev.xylonity.companions.common.blockentity.FrogBonanzaBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mixin(FrogBonanzaBlockEntity.class)
public abstract class FrogBonanzaMixin {

    @Unique
    private static final Logger comarcaconfig$LOGGER = LogUtils.getLogger();

    // Asegúrate de que este nombre sea exactamente igual al del mod original
    @Shadow @Final
    private int[] faceDeg;

    @Inject(method = "getFroggyReward", at = @At("HEAD"), cancellable = true)
    private void comarcaconfig$replaceRewards(CallbackInfo ci) {
        FrogBonanzaBlockEntity self = (FrogBonanzaBlockEntity)(Object)this;

        Level level = self.getLevel();
        if (!(level instanceof ServerLevel server)) return;

        BlockPos pos = self.getBlockPos();

        Map<Integer, Integer> counts = new HashMap<>();

        // Prevención de crasheos por si faceDeg no ha cargado
        if (faceDeg != null) {
            for (int f : faceDeg) {
                counts.put(f, counts.getOrDefault(f, 0) + 1);
            }
        }

        if (counts.size() == 1) {
            int face = faceDeg[0];
            switch (face) {
                case 0 -> comarcaconfig$handleTripleCreeper(server, pos);
                case 90 -> this.comarcaconfig$handleTripleCoin(server, pos);
                case 180 -> this.comarcaconfig$handleTripleTeddy(server, pos);
                case 270 -> this.comarcaconfig$handleTripleSkull(server, pos);
            }
        } else {
            for (var entry : counts.entrySet()) {
                if (entry.getValue() == 2) {
                    switch (entry.getKey()) {
                        case 0 -> comarcaconfig$handleDoubleCreeper(server, pos);
                        case 90 -> this.comarcaconfig$handleDoubleCoin(server, pos);
                        case 180 -> this.comarcaconfig$handleDoubleTeddy(server, pos);
                        case 270 -> this.comarcaconfig$handleDoubleSkull(server, pos);
                    }
                }
            }
        }

        ci.cancel(); // 🔥 cancela TODO lo original exitosamente
    }

    // CREEPERS
    @Unique
    private void comarcaconfig$handleDoubleCreeper(ServerLevel server, BlockPos pos) {
        RandomSource rand = server.random;

        try {
            Class<?> tntClass = Class.forName("dev.xylonity.companions.common.blockentity.FrogBonanzaBlockEntity$BonanzaTnt");
            var constructor = tntClass.getDeclaredConstructor(
                    net.minecraft.world.level.Level.class,
                    double.class,
                    double.class,
                    double.class,
                    net.minecraft.world.entity.LivingEntity.class
            );
            constructor.setAccessible(true);

            // RANGO DINÁMICO: 3 a 6 TNTs
            int amount = 3 + rand.nextInt(4);

            for (int i = 0; i < amount; ++i) {
                net.minecraft.world.entity.Entity tntEntity = (net.minecraft.world.entity.Entity) constructor.newInstance(
                        server, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, null
                );

                if (tntEntity instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
                    double angle = rand.nextDouble() * Math.PI * 2.0;
                    double speed = 0.15 + rand.nextDouble() * 0.6;

                    tnt.setDeltaMovement(Math.cos(angle) * speed, 0.5 + rand.nextDouble() * 0.3, Math.sin(angle) * speed);
                    tnt.setFuse(40);
                    server.addFreshEntity(tnt);
                }
            }
        } catch (Exception e) {
            comarcaconfig$LOGGER.error("[ComarcaConfig] Error al spawnear las TNTs: ", e);
        }

        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void comarcaconfig$handleTripleCreeper(ServerLevel server, BlockPos pos) {
        RandomSource rand = server.random;

        for (int i = 0; i < 3; ++i) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = rand.nextDouble() * 4.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * dist;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * dist;

            net.minecraft.world.entity.monster.Creeper creeper = EntityType.CREEPER.create(server);
            if (creeper != null) {
                creeper.moveTo(x, y, z, rand.nextFloat() * 360.0F, 0.0F);

                // Crear el rayo para hacerlo Charged Creeper
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(x, y, z);
                    server.addFreshEntity(bolt);
                    creeper.thunderHit(server, bolt); // Esto lo convierte en eléctrico
                }

                server.addFreshEntity(creeper);
            }
        }

        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    //LOOTS
    @Unique
    private void comarcaconfig$dropLoot(ServerLevel server, BlockPos pos, String lootPath) {
        ResourceLocation location = ResourceLocation.parse(lootPath);
        ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, location);

        // Obtenemos la tabla desde el registro del servidor
        LootTable table = server.getServer().reloadableRegistries().getLootTable(lootKey);

        // DEPURACIÓN: Si la tabla está vacía (no se encontró), lanza un mensaje en la consola
        if (table == LootTable.EMPTY) {
            System.out.println("ERROR: No se encontró la Loot Table en: " + location);
            return;
        }

        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);

        List<ItemStack> items = table.getRandomItems(params);

        if (items.isEmpty()) {
            System.out.println("La Loot Table " + location + " cargó bien, pero no generó items.");
        }

        for (ItemStack stack : items) {
            Containers.dropItemStack(server, pos.getX(), pos.getY() + 2.5, pos.getZ(), stack);
        }
    }

    @Unique
    private void comarcaconfig$handleDoubleCoin(ServerLevel server, BlockPos pos) {
        comarcaconfig$dropLoot(server, pos, "comarcaconfig:bonanza/double_coin");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void comarcaconfig$handleTripleCoin(ServerLevel server, BlockPos pos) {
        comarcaconfig$dropLoot(server, pos, "comarcaconfig:bonanza/triple_coin");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void comarcaconfig$handleDoubleTeddy(ServerLevel server, BlockPos pos) {
        comarcaconfig$dropLoot(server, pos, "comarcaconfig:bonanza/double_teddy");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void comarcaconfig$handleTripleTeddy(ServerLevel server, BlockPos pos) {
        comarcaconfig$dropLoot(server, pos, "comarcaconfig:bonanza/triple_teddy");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

    }

    // Metodos para acceder a efectos de otros mods

    @Unique
    private net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> comarcaconfig$getModEffect(String location) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
                ResourceLocation.parse(location)
        ).orElse(null);
    }

    //DOUBLE SKULL

    @Unique
    private void comarcaconfig$handleDoubleSkull(ServerLevel server, BlockPos pos) {
        // Buscamos al jugador más cercano primero, ya que casi todos los eventos lo usarán
        Player player = server.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
        if (player == null) return; // Si no hay nadie cerca, no hacemos nada

        List<Map.Entry<String,Integer>> eventos = List.of(
                Map.entry("anvil", 60),
                Map.entry("blind",40 ),
                Map.entry("lightning", 30),
                Map.entry("teleport", 25),
                Map.entry("stun", 15),
                Map.entry("kill", 9),
                Map.entry("backrooms",1)
        );

        int totalweight = eventos.stream().mapToInt(Map.Entry::getValue).sum();
        int r = server.random.nextInt(totalweight);

        String selectedEvent = "";
        int count = 0;
        for(Map.Entry<String, Integer> entry : eventos){
            count += entry.getValue();
            if (r < count){
                selectedEvent = entry.getKey();
                break;
            }
        }
        switch (selectedEvent) {
            case "anvil" -> comarcaconfig$spawnAnvilOnPlayer(server, player);
            case "lightning" -> comarcaconfig$strikeLightningOnPlayer(server, player);
            case "teleport" -> comarcaconfig$teleportPlayer(player);
            case "blind" -> comarcaconfig$blindPlayer(player);
            case "kill" -> comarcaconfig$killPlayer(player);
            case "stun" -> comarcaconfig$stunPlayer(player);
            case "backrooms" -> comarcaconfig$backroomsTp(player);
        }
    }

    @Unique
    private void comarcaconfig$spawnAnvilOnPlayer(ServerLevel server, Player player) {
        // 1. Mensaje de advertencia
        player.displayClientMessage(
                Component.literal("¡MIRA ARRIBA!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                true
        );

        // 2. Aplicar el STUN de Cataclysm
        var stunAnvil = comarcaconfig$getModEffect("cataclysm:stun");
        if (stunAnvil != null) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(stunAnvil, 100, 1));
        }

        try {
            // Obtenemos el constructor privado de FallingBlockEntity
            // Los parámetros son: Level, double, double, double, BlockState
            var constructor = FallingBlockEntity.class.getDeclaredConstructor(
                    net.minecraft.world.level.Level.class,
                    double.class,
                    double.class,
                    double.class,
                    net.minecraft.world.level.block.state.BlockState.class
            );

            // La llave maestra para saltar el "private"
            constructor.setAccessible(true);

            // Instanciamos el yunque
            FallingBlockEntity anvil = constructor.newInstance(
                    server,
                    player.getX(),
                    player.getY() + 10.0,
                    player.getZ(),
                    Blocks.ANVIL.defaultBlockState()
            );

            // Ahora sí podemos aplicar lo que aprendimos del mod original
            anvil.time = 1;
            anvil.disableDrop();
            anvil.setHurtsEntities(2.0f, 40);

            server.addFreshEntity(anvil);

        } catch (Exception e) {
            // CAMBIO AQUÍ: Logging robusto en lugar de printStackTrace
            comarcaconfig$LOGGER.error("[ComarcaConfig] Error al spawnear yunque por reflexión: ", e);

            // Plan B seguro
            FallingBlockEntity.fall(server, player.blockPosition().above(10), Blocks.ANVIL.defaultBlockState());
        }
    }

    @Unique
    private void comarcaconfig$strikeLightningOnPlayer(ServerLevel server, Player player) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(player.position()); // Directo al jugador
            server.addFreshEntity(bolt);
        }
    }

    @Unique
    private void comarcaconfig$teleportPlayer(Player player) {
        // Ejemplo: Teletransportar 100 bloques hacia arriba (caída libre)
        player.teleportTo(player.getX(), player.getY() + 100, player.getZ());
    }

    @Unique
    private void comarcaconfig$blindPlayer(Player player){
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        var paranoia = comarcaconfig$getModEffect("hominid:paranoia");

        if (paranoia != null) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(paranoia, 200, 1));
        }
    }

    @Unique
    private void comarcaconfig$killPlayer(Player player){
        player.displayClientMessage(
                Component.literal("¡Mala suerte!").withStyle(ChatFormatting.RED),
                true // 'true' hace que salga en la Action Bar
        );
        player.kill();
    }

    @Unique
    private void comarcaconfig$stunPlayer(Player player) {
        // Obtenemos el efecto del mod externo
        // Ejemplo: "modid:nombre_del_efecto"
        var stunsolo = comarcaconfig$getModEffect("cataclysm:stun");

        if (stunsolo != null) {
            // Creamos la instancia: (Efecto, duración en ticks, nivel)
            // 20 ticks = 1 segundo. 600 ticks = 30 segundos.
            // El nivel empieza en 0 (Nivel I)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(stunsolo, 200, 1));
        }
    }

    @Unique
    private void comarcaconfig$backroomsTp(Player player) {
        player.displayClientMessage(
                Component.literal("¡Bienvenido a los backrooms!").withStyle(ChatFormatting.RED),
                true
        );
        // Ejemplo: Teletransportar 100 bloques hacia arriba (caída libre)
        player.teleportTo(player.getX(), player.getY() - 3 , player.getZ());
    }

    // Metodo auxiliar para obtener entidades de otros mods de forma segura
    @Unique
    private EntityType<?> comarcaconfig$getModEntity(String location) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(location));
    }

    //TRIPLE SKULL

    @Unique
    private void comarcaconfig$handleTripleSkull(ServerLevel server, BlockPos pos) {
        record MobReward(EntityType<?> type, int weight, int count) {}
        List<MobReward> rewards = List.of(
                new MobReward(EntityType.ZOMBIE, 50,5),
                new MobReward(EntityType.WITHER_SKELETON,40,5),
                new MobReward(EntityType.CAVE_SPIDER,40,5),
                new MobReward(EntityType.CREEPER, 30,5),
                new MobReward(EntityType.BREEZE, 20,3),
                new MobReward(comarcaconfig$getModEntity("royalvariations:royal_creeper"), 15,1),
                new MobReward(comarcaconfig$getModEntity("royalvariations:royal_zombie"), 15,1),
                new MobReward(EntityType.WARDEN, 5,1)
        );

        // Selección por peso
        int totalWeight = rewards.stream().mapToInt(MobReward::weight).sum();
        int r = server.random.nextInt(totalWeight);

        MobReward selected = null;
        int current = 0;
        for (MobReward reward : rewards) {
            current += reward.weight();
            if (r < current) {
                selected = reward;
                break;
            }
        }

        // Spawneo múltiple
        if (selected != null && selected.type() != null) {
            //  posición base arriba de la máquina
            BlockPos spawnBase = pos.above();

            for (int i = 0; i < selected.count(); i++) {
                // desplazamiento
                double offsetX = (server.random.nextDouble() - 0.5) * 1.5;
                double offsetZ = (server.random.nextDouble() - 0.5) * 1.5;

                net.minecraft.world.phys.Vec3 finalPos = spawnBase.getBottomCenter().add(offsetX, 0, offsetZ);

                selected.type().spawn(server,
                        BlockPos.containing(finalPos), // Convertimos el Vec3 de vuelta a BlockPos para el spawn
                        MobSpawnType.MOB_SUMMONED
                );
            }
        }
    }


}