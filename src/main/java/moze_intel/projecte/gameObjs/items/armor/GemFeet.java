package moze_intel.projecte.gameObjs.items.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import moze_intel.projecte.gameObjs.items.IFlightProvider;
import moze_intel.projecte.gameObjs.items.IStepAssister;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemFeet extends GemArmorBase implements IFlightProvider, IStepAssister {

	private static final UUID MODIFIER = UUID.fromString("A4334312-DFF8-4582-9F4F-62AD0C070475");
	private static final Vec3 VERTICAL_MOVEMENT = new Vec3(0, 0.1, 0);

	private final Multimap<Attribute, AttributeModifier> attributes;

	public GemFeet(Properties props) {
		super(ArmorItem.Type.BOOTS, props);
		Builder<Attribute, AttributeModifier> attributesBuilder = ImmutableMultimap.builder();
		attributesBuilder.putAll(getDefaultAttributeModifiers(EquipmentSlot.FEET));
		attributesBuilder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MODIFIER, "Armor modifier", 1.0, Operation.MULTIPLY_TOTAL));
		this.attributes = attributesBuilder.build();
	}

	public void toggleStepAssist(ItemStack boots, Player player) {
		boolean value;
		CompoundTag bootsTag = boots.getOrCreateTag();
		if (bootsTag.contains(Constants.NBT_KEY_STEP_ASSIST, Tag.TAG_BYTE)) {
			value = !bootsTag.getBoolean(Constants.NBT_KEY_STEP_ASSIST);
			bootsTag.putBoolean(Constants.NBT_KEY_STEP_ASSIST, value);
		} else {
			//If we don't have the tag count that as it already being "false"
			bootsTag.putBoolean(Constants.NBT_KEY_STEP_ASSIST, true);
			value = true;
		}
		if (value) {
			player.sendSystemMessage(PELang.STEP_ASSIST.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED));
		} else {
			player.sendSystemMessage(PELang.STEP_ASSIST.translate(ChatFormatting.RED, PELang.GEM_DISABLED));
		}
	}

	private static boolean isJumpPressed() {
		if (FMLEnvironment.dist.isClient()) {
			return Minecraft.getInstance().player != null && Minecraft.getInstance().player.input.jumping;
		}
		return false;
	}

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, level, entity, slot, isHeld);
		if (isArmorSlot(slot) && entity instanceof Player player) {
			if (!level.isClientSide) {
				player.fallDistance = 0;
			} else {
				boolean flying = player.getAbilities().flying;
				if (!flying && isJumpPressed()) {
					player.addDeltaMovement(VERTICAL_MOVEMENT);
				}
				if (!player.onGround()) {
					if (player.getDeltaMovement().y() <= 0) {
						player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.9, 1));
					}
					if (!flying) {
						if (player.zza < 0) {
							player.setDeltaMovement(player.getDeltaMovement().multiply(0.9, 1, 0.9));
						} else if (player.zza > 0 && player.getDeltaMovement().lengthSqr() < 3) {
							player.setDeltaMovement(player.getDeltaMovement().multiply(1.1, 1, 1.1));
						}
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltips, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, level, tooltips, flags);
		tooltips.add(PELang.GEM_LORE_FEET.translate());
		tooltips.add(PELang.STEP_ASSIST_PROMPT.translate(ClientKeyHelper.getKeyName(PEKeybind.BOOTS_TOGGLE)));
		if (ItemHelper.checkItemNBT(stack, Constants.NBT_KEY_STEP_ASSIST)) {
			tooltips.add(PELang.STEP_ASSIST.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED));
		} else {
			tooltips.add(PELang.STEP_ASSIST.translate(ChatFormatting.RED, PELang.GEM_DISABLED));
		}
	}

	@NotNull
	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(@NotNull EquipmentSlot slot, ItemStack stack) {
		return slot == EquipmentSlot.FEET ? attributes : super.getAttributeModifiers(slot, stack);
	}

	@Override
	public boolean canProvideFlight(ItemStack stack, Player player) {
		return player.getItemBySlot(EquipmentSlot.FEET) == stack;
	}

	@Override
	public boolean canAssistStep(ItemStack stack, Player player) {
		return player.getItemBySlot(EquipmentSlot.FEET) == stack && ItemHelper.checkItemNBT(stack, Constants.NBT_KEY_STEP_ASSIST);
	}
}