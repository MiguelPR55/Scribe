package com.scribe.network;

import com.scribe.Scribe;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Handles server-to-client networking for waypoints synchronization.
 */
public final class ScribeNetworking {

	private ScribeNetworking() {
	}

	public record WaypointsSyncPayload(List<CoordinateEntry> entries) implements CustomPacketPayload {

		public static final CustomPacketPayload.Type<WaypointsSyncPayload> TYPE =
				new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Scribe.MOD_ID, "sync_waypoints"));

		private static final StreamCodec<ByteBuf, CoordinateEntry> ENTRY_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, CoordinateEntry::name,
				BlockPos.STREAM_CODEC, CoordinateEntry::pos,
				ByteBufCodecs.STRING_UTF8, CoordinateEntry::dimension,
				CoordinateEntry::new
		);

		public static final StreamCodec<ByteBuf, WaypointsSyncPayload> CODEC = StreamCodec.composite(
				ENTRY_CODEC.apply(ByteBufCodecs.list()), WaypointsSyncPayload::entries,
				WaypointsSyncPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Registers common payload type and server player join listener.
	 */
	public static void initCommon() {
		PayloadTypeRegistry.clientboundPlay().register(WaypointsSyncPayload.TYPE, WaypointsSyncPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, WaypointsSyncPayload.TYPE)) {
				ServerPlayNetworking.send(handler.getPlayer(), new WaypointsSyncPayload(CoordinateStorage.all()));
			}
		});
	}

	/**
	 * Broadcasts current waypoints to all connected clients supporting Scribe.
	 */
	public static void broadcastSync(MinecraftServer server) {
		if (server == null) {
			return;
		}
		WaypointsSyncPayload payload = new WaypointsSyncPayload(CoordinateStorage.all());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, WaypointsSyncPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}
}
