package net.citizensnpcs.npc.entity;

import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.MobEntityController;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.HorseModifiers;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_7_R2.EntityHorse;
import net.minecraft.server.v1_7_R2.World;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R2.CraftServer;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftHorse;
import org.bukkit.entity.Horse;
import org.bukkit.util.Vector;

public class HorseController extends MobEntityController {
    public HorseController() {
        super(EntityHorseNPC.class);
    }

    @Override
    public Horse getBukkitEntity() {
        return (Horse) super.getBukkitEntity();
    }

    @Override
    public void spawn(Location at, NPC npc) {
        npc.getTrait(HorseModifiers.class);
        super.spawn(at, npc);
    }

    public static class EntityHorseNPC extends EntityHorse implements NPCHolder {
        private int jumpTicks;
        private final CitizensNPC npc;

        public EntityHorseNPC(World world) {
            this(world, null);
        }

        public EntityHorseNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                NMS.clearGoals(goalSelector, targetSelector);
                ((Horse) getBukkitEntity()).setDomestication(((Horse) getBukkitEntity()).getMaxDomestication());
            }
        }

        @Override
        public void a(boolean flag) {
            float oldw = width;
            float oldl = length;
            super.a(flag);
            if (oldw != width || oldl != length) {
                this.setPosition(locX - 0.01, locY, locZ - 0.01);
                this.setPosition(locX + 0.01, locY, locZ + 0.01);
            }
        }

        @Override
        protected void a(double d0, boolean flag) {
            if (npc == null || !npc.isFlyable()) {
                super.a(d0, flag);
            }
        }

        @Override
        protected String aS() {
            return npc == null ? super.aS() : npc.data().get(NPC.HURT_SOUND_METADATA, super.aS());
        }

        @Override
        protected String aT() {
            return npc == null ? super.aT() : npc.data().get(NPC.DEATH_SOUND_METADATA, super.aT());
        }

        @Override
        protected void b(float f) {
            if (npc == null || !npc.isFlyable()) {
                super.b(f);
            }
        }

        @Override
        public boolean bN() {
            if (npc == null)
                return super.bN();
            boolean protectedDefault = npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
            if (!protectedDefault || !npc.data().get(NPC.LEASH_PROTECTED_METADATA, protectedDefault))
                return super.bN();
            if (super.bN()) {
                unleash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        public void collide(net.minecraft.server.v1_7_R2.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public void e() {
            if (npc == null) {
                super.e();
            } else {
                NMS.setStepHeight(this, 1);
                updateAIWithMovement();
                npc.update();
            }
        }

        @Override
        public void e(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                super.e(f, f1);
            } else {
                NMS.flyingMoveLogic(this, f, f1);
            }
        }

        @Override
        public void g(double x, double y, double z) {
            if (npc == null) {
                super.g(x, y, z);
                return;
            }
            if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0) {
                if (!npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true))
                    super.g(x, y, z);
                return;
            }
            Vector vector = new Vector(x, y, z);
            NPCPushEvent event = Util.callPushEvent(npc, vector);
            if (!event.isCancelled()) {
                vector = event.getCollisionVector();
                super.g(vector.getX(), vector.getY(), vector.getZ());
            }
            // when another entity collides, this method is called to push the
            // NPC so we prevent it from doing anything if the event is
            // cancelled.
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (bukkitEntity == null && npc != null)
                bukkitEntity = new HorseNPC(this);
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public boolean h_() {
            if (npc == null || !npc.isFlyable()) {
                return super.h_();
            } else {
                return false;
            }
        }

        @Override
        protected String t() {
            return npc == null ? super.aS() : npc.data().get(NPC.AMBIENT_SOUND_METADATA, super.t());
        }

        private void updateAIWithMovement() {
            NMS.updateAI(this);
            // taken from EntityLiving update method
            if (bc) {
                /* boolean inLiquid = H() || J();
                 if (inLiquid) {
                     motY += 0.04;
                 } else //(handled elsewhere)*/
                if (onGround && jumpTicks == 0) {
                    bi();
                    jumpTicks = 10;
                }
            } else {
                jumpTicks = 0;
            }
            bd *= 0.98F;
            be *= 0.98F;
            bf *= 0.9F;

            e(bd, be); // movement method
            NMS.setHeadYaw(this, yaw);
            if (jumpTicks > 0) {
                jumpTicks--;
            }
        }

        @Override
        protected void w() {
            if (npc == null) {
                super.w();
            }
        }
    }

    public static class HorseNPC extends CraftHorse implements NPCHolder {
        private final CitizensNPC npc;

        public HorseNPC(EntityHorseNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}