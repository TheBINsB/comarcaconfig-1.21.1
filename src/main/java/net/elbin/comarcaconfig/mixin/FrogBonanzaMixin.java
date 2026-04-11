package net.elbin.comarcaconfig.mixin;

import dev.xylonity.companions.common.blockentity.FrogBonanzaBlockEntity;
import dev.xylonity.companions.registry.CompanionsSounds;

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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(FrogBonanzaBlockEntity.class)
public abstract class FrogBonanzaMixin {

    // Asegúrate de que este nombre sea exactamente igual al del mod original
    @Shadow
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
                case 0 -> handleTripleCreeper(server, pos);
                case 90 -> this.handleTripleCoin(server, pos);
                case 180 -> this.handleTripleTeddy(server, pos);
                case 270 -> this.handleTripleSkull(server, pos);
            }
        } else {
            for (var entry : counts.entrySet()) {
                if (entry.getValue() == 2) {
                    switch ((Integer)entry.getKey()) {
                        case 0 -> handleDoubleCreeper(server, pos);
                        case 90 -> this.handleDoubleCoin(server, pos);
                        case 180 -> this.handleDoubleTeddy(server, pos);
                        case 270 -> this.handleDoubleSkull(server, pos);
                    }
                }
            }
        }

        ci.cancel(); // 🔥 cancela TODO lo original exitosamente
    }

    // CREEPERS
    private void handleDoubleCreeper(ServerLevel server, BlockPos pos) {
        BlockPos center = pos.above();
        RandomSource rand = server.random;

        try {
            // Buscamos la clase privada dentro de FrogBonanzaBlockEntity
            Class<?> tntClass = Class.forName("dev.xylonity.companions.common.blockentity.FrogBonanzaBlockEntity$BonanzaTnt");

            // Buscamos el constructor: (Level, double, double, double, LivingEntity)
            var constructor = tntClass.getDeclaredConstructor(
                    net.minecraft.world.level.Level.class,
                    double.class,
                    double.class,
                    double.class,
                    net.minecraft.world.entity.LivingEntity.class
            );

            // Lo hacemos accesible (esto es lo que se salta el 'private')
            constructor.setAccessible(true);

            for (int i = 0; i < 2 + rand.nextInt(3); ++i) {
                // Instanciamos la clase
                net.minecraft.world.entity.Entity tntEntity = (net.minecraft.world.entity.Entity) constructor.newInstance(
                        server, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, null
                );

                // Como BonanzaTnt extiende de PrimedTnt, podemos castearlo a PrimedTnt
                // para usar los métodos de movimiento y mecha
                if (tntEntity instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
                    double angle = rand.nextDouble() * Math.PI * 2.0;
                    double speed = 0.15 + rand.nextDouble() * 0.6;

                    tnt.setDeltaMovement(Math.cos(angle) * speed, 0.5 + rand.nextDouble() * 0.3, Math.sin(angle) * speed);
                    tnt.setFuse(40);
                    server.addFreshEntity(tnt);
                }
            }
        } catch (Exception e) {
            // Si algo falla (clase no encontrada, etc.), imprimimos el error en consola
            e.printStackTrace();
        }

        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void handleTripleCreeper(ServerLevel server, BlockPos pos) {
        BlockPos center = pos.above();
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
    private void dropLoot(ServerLevel server, BlockPos pos, String lootPath) {
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
            Containers.dropItemStack(server, pos.getX(), pos.getY() + 1.1, pos.getZ(), stack);
        }
    }

    private void handleDoubleCoin(ServerLevel server, BlockPos pos) {
        dropLoot(server, pos, "comarcaconfig:bonanza/double_coin");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void handleTripleCoin(ServerLevel server, BlockPos pos) {
        dropLoot(server, pos, "comarcaconfig:bonanza/triple_coin");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void handleDoubleTeddy(ServerLevel server, BlockPos pos) {
        dropLoot(server, pos, "comarcaconfig:bonanza/double_teddy");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void handleTripleTeddy(ServerLevel server, BlockPos pos) {
        dropLoot(server, pos, "comarcaconfig:bonanza/triple_teddy");
        server.playSound(null, pos, dev.xylonity.companions.registry.CompanionsSounds.POP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

    }

    // Metodos para acceder a efectos de otros mods

    private net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> getModEffect(String location) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
                ResourceLocation.parse(location)
        ).orElse(null);
    }

    //DOUBLE SKULL

    private void handleDoubleSkull(ServerLevel server, BlockPos pos) {
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
            case "anvil" -> spawnAnvilOnPlayer(server, player);
            case "lightning" -> strikeLightningOnPlayer(server, player);
            case "teleport" -> teleportPlayer(player);
            case "blind" -> blindPlayer(player);
            case "kill" -> killPlayer(player);
            case "stun" -> stunPlayer(player);
            case "backrooms" -> backroomsTp(player);
        }
    }

    private void spawnAnvilOnPlayer(ServerLevel server, Player player) {

        player.displayClientMessage(
                Component.literal("¡Mira arriba!").withStyle(ChatFormatting.RED),
                true // 'true' hace que salga en la Action Bar
        );

        BlockPos spawnPos = player.blockPosition().above(5);
        FallingBlockEntity anvil = FallingBlockEntity.fall(server, spawnPos, Blocks.ANVIL.defaultBlockState());

        var stunAnvil = getModEffect("cataclysm:stun");

        if (stunAnvil != null) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(stunAnvil, 200, 1));
        }

        anvil.setHurtsEntities(2.0f, 40);
        anvil.dropItem = false; // Para que no suelte el ítem al romperse:

        server.addFreshEntity(anvil);
    }

    private void strikeLightningOnPlayer(ServerLevel server, Player player) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(player.position()); // Directo al jugador
            server.addFreshEntity(bolt);
        }
    }

    private void teleportPlayer(Player player) {
        // Ejemplo: Teletransportar 100 bloques hacia arriba (caída libre)
        player.teleportTo(player.getX(), player.getY() + 100, player.getZ());
    }

    private void blindPlayer(Player player){
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
    }

    private void killPlayer(Player player){
        player.displayClientMessage(
                Component.literal("¡Mala suerte!").withStyle(ChatFormatting.RED),
                true // 'true' hace que salga en la Action Bar
        );
        player.kill();
    }

    private void stunPlayer(Player player) {
        // Obtenemos el efecto del mod externo
        // Ejemplo: "modid:nombre_del_efecto"
        var stunsolo = getModEffect("cataclysm:stun");

        if (stunsolo != null) {
            // Creamos la instancia: (Efecto, duración en ticks, nivel)
            // 20 ticks = 1 segundo. 600 ticks = 30 segundos.
            // El nivel empieza en 0 (Nivel I)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(stunsolo, 600, 1));
        }
    }

    private void backroomsTp(Player player) {
        player.displayClientMessage(
                Component.literal("¡Bienvenido a los backrooms!").withStyle(ChatFormatting.RED),
                true // 'true' hace que salga en la Action Bar
        );
        // Ejemplo: Teletransportar 100 bloques hacia arriba (caída libre)
        player.teleportTo(player.getX(), player.getY() - 3 , player.getZ());
    }

    // Metodo auxiliar para obtener entidades de otros mods de forma segura
    private EntityType<?> getModEntity(String location) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(location));
    }

    //TRIPLE SKULL

    private void handleTripleSkull(ServerLevel server, BlockPos pos) {
        List<Map.Entry<EntityType<?>, Integer>> mobs = List.of(
                Map.entry(EntityType.BREEZE, 20),
                Map.entry(EntityType.ZOMBIE, 50),
                Map.entry(EntityType.WITHER_SKELETON,40),
                Map.entry(EntityType.CREEPER, 30),
                Map.entry(EntityType.WARDEN, 5),
                Map.entry(getModEntity("companions:cornelius"), 15)
        );

        EntityType<?> selected = getWeightedRandom(server.random, mobs);
        if (selected != null) {
            selected.spawn(server, pos.above(), MobSpawnType.MOB_SUMMONED);
        }
    }

    private EntityType<?> getWeightedRandom(RandomSource rand, List<Map.Entry<EntityType<?>, Integer>> list) {
        // Sumamos todos los pesos (los valores de la derecha)
        int total = list.stream().mapToInt(Map.Entry::getValue).sum();
        int r = rand.nextInt(total);

        int count = 0;
        for (Map.Entry<EntityType<?>, Integer> entry : list) {
            count += entry.getValue();
            if (r < count) return entry.getKey(); // Devolvemos la Entidad (el valor de la izquierda)
        }

        return null;
    }


}