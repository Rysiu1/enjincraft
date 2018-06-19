package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.identities.IdentitiesService;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.identities.vo.data.CreateIdentityData;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.CreateUserData;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.UsersData;
import com.enjin.enjincoin.sdk.client.service.identities.vo.IdentityField;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.Bootstrap;
import com.enjin.enjincoin.spigot_framework.controllers.SdkClientController;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.util.UuidUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * <p>Link command handler.</p>
 */
public class LinkCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Link command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public LinkCommand(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args the command arguments
     *
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        UUID uuid = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            uuid = player.getUniqueId();
        } else {
            if (args.length >= 1) {
                try {
                    uuid = UuidUtils.stringToUuid(args[0]);
                } catch (IllegalArgumentException e) {
                    errorInvalidUuid(sender);
                }
            } else {
                final TextComponent text = TextComponent.of("UUID argument required.")
                        .color(TextColor.RED);
                MessageUtils.sendMessage(sender, text);
            }
        }

        if (uuid != null) {
            linkIdentity(sender, uuid);
        } else {
            errorInvalidUuid(sender);
        }
    }

    /**
     * <p>Starts the linking process for an identity if not
     * already linked.</p>
     *
     * @param sender the command sender
     * @param uuid the target player's UUID
     *
     * @since 1.0
     */
    private void linkIdentity(CommandSender sender, UUID uuid) {
        Bootstrap bootstrap = this.main.getBootstrap();
        SdkClientController controller = bootstrap.getSdkController();
        Client client = controller.getClient();
        IdentitiesService service = client.getIdentitiesService();

//        client.auth("");
        // TODO resolve this after identifying the start point for a user auth experience
        Callback callback = new FetchIdentityCallback(sender, uuid);

        service.getIdentitiesAsync(null, "", callback);
//        service.getIdentitiesAsync(new HashMap<String, Object>() {{
//            put("uuid", uuid);
//        }}, new FetchIdentityCallback(sender, uuid));
    }

    /**
     * <p>Sends an invalid uuid error message to the sender.</p>
     *
     * @param sender the command sender
     *
     * @since 1.0
     */
    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * <p>Sends a requesting identities error message to the sender.</p>
     *
     * @param sender the command sender
     *
     * @since 1.0
     */
    private void errorRequestingIdentities(CommandSender sender, Throwable t) {
        final TextComponent text = TextComponent.of("An error occurred while requesting a player identity.")
                .color(TextColor.RED);
        this.main.getLogger().log(Level.WARNING, t.getMessage(), t);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * <p>Sends a creating identities error message to the sender.</p>
     *
     * @param sender the command sender
     *
     * @since 1.0
     */
    private void errorCreatingIdentity(CommandSender sender, Throwable t) {
        final TextComponent text = TextComponent.of("An error occurred while creating a player identity.");
        this.main.getLogger().log(Level.WARNING, t.getMessage(), t);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * <p>Sends a link already exists error message to the sender.</p>
     *
     * @param sender the command sender
     *
     * @since 1.0
     */
    private void errorLinkAlreadyExists(CommandSender sender, UUID uuid) {
        final TextComponent text = TextComponent.of("An identity has already been linked to ")
                .color(TextColor.RED)
                .append(TextComponent.of(uuid.toString())
                        .color(TextColor.GOLD));
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * <p>Sends a message to verify whether a link code was
     * acquired or not and if so sends the link code to the sender.</p>
     *
     * @param sender the command sender
     * @param code the link code
     *
     * @since 1.0
     */
    private void handleCode(CommandSender sender, String code) {
        if (code == null || code.isEmpty()) {
            final TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present.")
                            .color(TextColor.GOLD));
            MessageUtils.sendMessage(sender, text);
        } else {
            final TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(code)
                            .color(TextColor.GOLD));
            MessageUtils.sendMessage(sender, text);
        }
    }

    /**
     * <p>Base callback for link related requests.</p>
     *
     * @param <T> the return type
     *
     * @since 1.0
     */
    public abstract class CallbackBase<T> implements Callback<T> {

        /**
         * <p>The identities service.</p>
         */
        private IdentitiesService service;

        /**
         * <p>The command sender.</p>
         */
        private CommandSender sender;

        /**
         * <p>The target player's UUID.</p>
         */
        private UUID uuid;

        /**
         * <p>Callback constructor.</p>
         *
         * @param sender the command sender
         * @param uuid the target player's UUID
         */
        public CallbackBase(CommandSender sender, UUID uuid) {
            this.service = LinkCommand.this.main.getBootstrap().getSdkController().getClient().getIdentitiesService();
            this.sender = sender;
            this.uuid = uuid;
        }

        /**
         * <p>Returns the identities service.</p>
         *
         * @return the identities service
         *
         * @since 1.0
         */
        public IdentitiesService getService() {
            return service;
        }

        /**
         * <p>Returns the command sender.</p>
         *
         * @return the command sender
         *
         * @since 1.0
         */
        public CommandSender getSender() {
            return sender;
        }

        /**
         * <p>Returns the target player's UUID.</p>
         *
         * @return the target player's UUID
         *
         * @since 1.0
         */
        public UUID getUuid() {
            return uuid;
        }
    }

    public class FetchUsersDataCallback extends CallbackBase<UsersData> {

        public FetchUsersDataCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<UsersData> call, Response<UsersData> response) {

        }

        @Override
        public void onFailure(Call<UsersData> call, Throwable t) {

        }
    }

    public class CreateEnjinUserCallBack extends CallbackBase<GraphQLResponse<CreateUserData>> {

        /**
         * <p>Callback constructor.</p>
         *
         * @param sender the command sender
         * @param uuid   the target player's UUID
         */
        public CreateEnjinUserCallBack(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<GraphQLResponse<CreateUserData>> call, Response<GraphQLResponse<CreateUserData>> response) {
            if (response.isSuccessful()) {
                if (getSender() instanceof Player && !((Player) getSender()).isOnline())
                    return;

//                Integer userId = response.body().getData().getUser().getId();
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<GraphQLResponse<CreateUserData>> call, Throwable t) {

        }
    }

    public class FetchEnjinUserCallback extends CallbackBase<GraphQLResponse<UsersData>> {

        /**
         * <p>Callback constructor.</p>
         *
         * @param sender the command sender
         * @param uuid   the target player's UUID
         */
        public FetchEnjinUserCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<GraphQLResponse<UsersData>> call, Response<GraphQLResponse<UsersData>> response) {
            if (response.isSuccessful()) {
                if (getSender() instanceof Player && !((Player) getSender()).isOnline())
                    return;

//                String name = response.body().getData().getUsers().get(0).getName();
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<GraphQLResponse<UsersData>> call, Throwable t) {
            errorCreatingIdentity(getSender(), t);
        }
    }

    /**
     * <p>Callback that handles fetched identities and either creates
     * an identity if one does not exists or handles the link code of
     * an existing identity.</p>
     *
     * @since 1.0
     */
    public class FetchIdentityCallback extends CallbackBase<Identity[]> {

        /**
         * <p>Callback constructor.</p>
         *
         * @param sender the command sender
         * @param uuid the target player's UUID
         */
        public FetchIdentityCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<Identity[]> call, Response<Identity[]> response) {
            if (response.isSuccessful()) {
                Identity[] identities = response.body();
                if (identities.length == 0) {
                    // TODO: App ID needs to be configurable or acquired by some means.

                    Integer id = 0;
                    String ethereumAddress = "";
                    List<IdentityField> fields = new ArrayList<>();
                    fields.add( new IdentityField("uuid", getUuid().toString()) );

                    // final Integer id, final String ethereumAddress, final List<IdentityField> fields, final Callback<GraphQLResponse<CreateIdentityData>> callback
                    getService().createIdentityAsync(id, ethereumAddress, fields, new CreateIdentityCallback(getSender(), getUuid()));
//                            new CreateIdentityRequestBody(main.getBootstrap().getConfig().get("appId").getAsInt(), new IdentityField[]{
//                                    new IdentityField("uuid", getUuid().toString())
//                            }),
//                            new CreateIdentityCallback(getSender(), getUuid()));
                } else {
                    Identity identity = identities[0];
                    String code = identity.getLinkingCode();
                    if (code == null || code.isEmpty())
                        errorLinkAlreadyExists(getSender(), getUuid());
                    else
                        handleCode(getSender(), code);
                }
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<Identity[]> call, Throwable t) {
            errorRequestingIdentities(getSender(), t);
        }
    }

    /**
     * <p>Callback that sends the linking code of the newly created
     * identity to the command sender.</p>
     *
     * @since 1.0
     */
    public class CreateIdentityCallback extends CallbackBase<GraphQLResponse<CreateIdentityData>> {

        /**
         * <p>Callback constructor.</p>
         *
         * @param sender the command sender
         * @param uuid the target player's UUID
         */
        public CreateIdentityCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<GraphQLResponse<CreateIdentityData>> call, Response<GraphQLResponse<CreateIdentityData>> response) {
            if (response.isSuccessful()) {
                if (getSender() instanceof Player && !((Player) getSender()).isOnline())
                    return;

                String code = response.body().getData().getIdentity().getLinkingCode();
                handleCode(getSender(), code);
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<GraphQLResponse<CreateIdentityData>> call, Throwable t) {
            errorCreatingIdentity(getSender(), t);
        }
    }

}