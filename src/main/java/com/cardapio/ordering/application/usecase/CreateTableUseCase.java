package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.CreateTableCommand;
import com.cardapio.ordering.application.dto.TableView;
import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.port.QrCodeGenerator;
import com.cardapio.ordering.domain.port.QrStorage;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class CreateTableUseCase {

    private final TableRepository tables;
    private final QrCodeGenerator qrGenerator;
    private final QrStorage qrStorage;
    private final Clock clock;

    @Value("${qr.size-px:512}")
    private int qrSizePx;

    @Transactional
    public Result<TableView> execute(CreateTableCommand cmd) {
        if (tables.findByNumber(cmd.number()).isPresent()) {
            return Result.failWith(ErrorCode.TABLE_NUMBER_TAKEN);
        }
        Table table = Table.create(cmd.number(), clock);
        tables.save(table);

        byte[] png = qrGenerator.generatePng(table.qrToken().toString(), qrSizePx);
        String key = "tables/" + table.id().value() + ".png";
        qrStorage.putIfAbsent(key, png, "image/png");
        table.attachQrImageKey(key, clock);
        tables.save(table);

        return Result.success(toView(table, false));
    }

    static TableView toView(Table table, boolean hasOpenComanda) {
        return new TableView(table.id(), table.number(), table.qrToken(), table.isActive(), hasOpenComanda);
    }
}
