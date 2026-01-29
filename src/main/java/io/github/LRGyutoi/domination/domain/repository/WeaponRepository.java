package io.github.LRGyutoi.domination.domain.repository;

import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;

import java.util.Collection;
import java.util.Optional;

/**
 * 武器リポジトリインターフェース
 * 武器定義の永続化・取得を担当
 */
public interface WeaponRepository {

    /**
     * 武器を保存
     */
    void save(Weapon weapon);

    /**
     * 武器IDで検索
     */
    Optional<Weapon> findById(WeaponId id);

    /**
     * 武器名で検索
     */
    Optional<Weapon> findByName(String name);

    /**
     * 全武器を取得
     */
    Collection<Weapon> findAll();

    /**
     * 設定ファイルからリロード
     */
    void reload();

    /**
     * 武器が存在するか確認
     */
    boolean exists(WeaponId id);
}
