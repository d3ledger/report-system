package jp.co.soramitsu.d3.reportsystem.datacollector;

import iroha.protocol.*;
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3;
import jp.co.soramitsu.iroha.java.IrohaAPI;
import jp.co.soramitsu.iroha.java.Query;
import jp.co.soramitsu.iroha.java.Transaction;
import jp.co.soramitsu.iroha.java.TransactionStatusObserver;
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver;
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer;
import jp.co.soramitsu.iroha.testcontainers.PeerConfig;
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestIrohaLib {

    private static final String bankDomain = "bank";
    private static final String userRole = "user";
    private static final String usdName = "usd";

    private static final Ed25519Sha3 crypto = new Ed25519Sha3();

    private static final KeyPair peerKeypair = crypto.generateKeypair();

    private static final KeyPair useraKeypair = crypto.generateKeypair();
    private static final KeyPair userbKeypair = crypto.generateKeypair();

    private static String user(String name) {
        return String.format("%s@%s", name, bankDomain);
    }

    private static final String usd = String.format("%s#%s", usdName, bankDomain);


    @Test
    public void testSomethingWithIroha() {
        IrohaContainer iroha = new IrohaContainer()
                .withPeerConfig(getPeerConfig());
        // start the peer. blocking call
        iroha.start();

        // create API wrapper
        IrohaAPI api = new IrohaAPI(iroha.getToriiAddress());

        // transfer 100 usd from user_a to user_b
        TransactionOuterClass.Transaction tx = Transaction.builder("user_a@bank")
                .transferAsset("user_a@bank", "user_b@bank", usd, "For pizza", "10")
                .sign(useraKeypair)
                .build();

        // create transaction observer
        // here you can specify any kind of handlers on transaction statuses
        InlineTransactionStatusObserver observer = TransactionStatusObserver.builder()
                // executed when stateless or stateful validation is failed
                .onTransactionFailed(t -> System.out.println(String.format(
                        "transaction %s failed with msg: %s",
                        t.getTxHash(),
                        t.getErrOrCmdName()
                )))
                // executed when got any exception in handlers or grpc
                .onError(e -> System.out.println("Failed with exception: " + e))
                // executed when we receive "committed" status
                .onTransactionCommitted((t) -> System.out.println("Committed :)"))
                // executed when transfer is complete (failed or succeed) and observable is closed
                .onComplete(() -> System.out.println("Complete"))
                .build();

        // blocking send.
        // use .subscribe() for async sending
        api.transaction(tx)
                .blockingSubscribe(observer);

        /// now lets query balances
        int balanceUserA = getBalance(api, user("user_a"), useraKeypair);
        int balanceUserB = getBalance(api, user("user_b"), userbKeypair);

        // ensure we got correct balances
        assert balanceUserA == 90;
        assert balanceUserB == 10;

    }

    /**
     * Custom facade over GRPC Query
     */
    public static int getBalance(IrohaAPI api, String userId, KeyPair keyPair) {
        // build protobuf query, sign it
        Queries.Query q = Query.builder(userId, 1)
                .getAccountAssets(userId)
                .buildSigned(keyPair);

        // execute query, get response
        QryResponses.QueryResponse res = api.query(q);

        // get list of assets from our response
        List<QryResponses.AccountAsset> assets = res.getAccountAssetsResponse().getAccountAssetsList();

        // find usd asset
        Optional<QryResponses.AccountAsset> assetUsdOptional = assets
                .stream()
                .filter(a -> a.getAssetId().equals(usd))
                .findFirst();

        // numbers are small, so we use int here for simplicity
        return assetUsdOptional
                .map(a -> Integer.parseInt(a.getBalance()))
                .orElse(0);
    }


    public static PeerConfig getPeerConfig() {
        PeerConfig config = PeerConfig.builder()
                .genesisBlock(getGenesisBlock())
                .build();

        // don't forget to add peer keypair to config
        config.withPeerKeyPair(peerKeypair);

        return config;
    }

    private static BlockOuterClass.Block getGenesisBlock() {
        return new GenesisBlockBuilder()
                // first transaction
                .addTransaction(
                        // transactions in genesis block can have no creator
                        Transaction.builder(null)
                                // by default peer is listening on port 10001
                                .addPeer("0.0.0.0:10001", peerKeypair.getPublic())
                                // create default "user" role
                                .createRole(userRole,
                                        Arrays.asList(
                                                Primitive.RolePermission.can_transfer,
                                                Primitive.RolePermission.can_get_my_acc_ast,
                                                Primitive.RolePermission.can_get_my_txs,
                                                Primitive.RolePermission.can_receive
                                        )
                                )
                                .createDomain(bankDomain, userRole)
                                // create user A
                                .createAccount("user_a", bankDomain, useraKeypair.getPublic())
                                // create user B
                                .createAccount("user_b", bankDomain, userbKeypair.getPublic())
                                // create usd#bank with precision 2
                                .createAsset(usdName, bankDomain, 2)
                                // transactions in genesis block can be unsigned
                                .build() // returns ipj model Transaction
                                .build() // returns unsigned protobuf Transaction
                )
                // we want to increase user_a balance by 100 usd
                .addTransaction(
                        Transaction.builder(user("user_a"))
                                .addAssetQuantity(usd, new BigDecimal("100"))
                                .build()
                                .build()
                )
                .build();
    }
}
