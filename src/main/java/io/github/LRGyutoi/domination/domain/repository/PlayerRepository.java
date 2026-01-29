package io.github.LRGyutoi.domination.domain.repository;

import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;

import java.util.Collection;
import java.util.Optional;

/**
 * プレイヤーリポジトリインターフェース
 * ドメイン層で定義し、インフラ層で実装
 */
public interface PlayerRepository {

    /**
     * プレイヤーを保存
     */
    void save(GamePlayer player);

    /**
     * プレイヤーIDで検索
     */
    Optional<GamePlayer> findById(PlayerId id);

    /**
     * 全プレイヤーを取得
     */
    Collection<GamePlayer> findAll();

    /**
     * ゲーム中のプレイヤーのみを取得
     */
    Collection<GamePlayer> findAllInGame();

    /**
     * プレイヤーを削除
     */
    void remove(PlayerId id);

    /**
     * 全プレイヤーを削除
     */
    void clear();

    /**
     * プレイヤーが存在するか確認
     */
    boolean exists(PlayerId id);
}
