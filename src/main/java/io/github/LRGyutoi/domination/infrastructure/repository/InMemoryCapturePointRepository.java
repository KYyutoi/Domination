package io.github.LRGyutoi.domination.infrastructure.repository;

import io.github.LRGyutoi.domination.domain.model.capture.CapturePoint;
import io.github.LRGyutoi.domination.domain.repository.CapturePointRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * インメモリCapturePointRepository実装
 */
public class InMemoryCapturePointRepository implements CapturePointRepository {

    private final Map<String, CapturePoint> capturePoints = new ConcurrentHashMap<>();

    @Override
    public void save(CapturePoint point) {
        capturePoints.put(point.getName(), point);
    }

    @Override
    public Optional<CapturePoint> findByName(String name) {
        return Optional.ofNullable(capturePoints.get(name));
    }

    @Override
    public Collection<CapturePoint> findAll() {
        return capturePoints.values();
    }

    @Override
    public void remove(String name) {
        capturePoints.remove(name);
    }

    @Override
    public void clear() {
        capturePoints.clear();
    }

    @Override
    public void reload() {
        // 設定ファイルからの再読み込み（サブクラスで実装）
    }
}
