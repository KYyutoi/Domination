package io.github.LRGyutoi.domination.domain.repository;

import io.github.LRGyutoi.domination.domain.model.capture.CapturePoint;

import java.util.Collection;
import java.util.Optional;

/**
 * 拠点リポジトリインターフェース
 */
public interface CapturePointRepository {

    /**
     * 拠点を保存
     */
    void save(CapturePoint point);

    /**
     * 拠点名で検索
     */
    Optional<CapturePoint> findByName(String name);

    /**
     * 全拠点を取得
     */
    Collection<CapturePoint> findAll();

    /**
     * 拠点を削除
     */
    void remove(String name);

    /**
     * 全拠点を削除
     */
    void clear();

    /**
     * 設定ファイルからリロード
     */
    void reload();
}
