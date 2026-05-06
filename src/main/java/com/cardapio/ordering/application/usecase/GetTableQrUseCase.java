package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.TableQrView;
import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.domain.port.QrStorage;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GetTableQrUseCase {

    private final TableRepository tables;
    private final QrStorage qrStorage;
    private final Clock clock;

    @Value("${qr.presigned-url-ttl-seconds:600}")
    private long ttlSeconds;

    @Transactional(readOnly = true)
    public Result<TableQrView> execute(TableId tableId) {
        Table table = tables.findById(tableId).orElse(null);
        if (table == null) return Result.failWith(ErrorCode.TABLE_NOT_FOUND);
        if (table.qrImageKey() == null) {
            return Result.failWith(ErrorCode.TABLE_NOT_FOUND, "qr image not yet generated");
        }
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        var url = qrStorage.presignedUrl(table.qrImageKey(), ttl);
        return Result.success(new TableQrView(url, clock.instant().plus(ttl)));
    }
}
