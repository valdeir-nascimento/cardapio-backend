package com.cardapio.shared.domain;

public enum ErrorCode {

    // Generic / shared
    INVALID_INPUT("entrada inválida"),

    // Identity
    INVALID_CREDENTIALS("credenciais inválidas"),
    INVALID_REFRESH_TOKEN("refresh token inválido"),
    EMAIL_ALREADY_REGISTERED("este e-mail já está cadastrado"),
    INVALID_EMAIL("e-mail inválido"),
    INVALID_PHONE("telefone inválido"),
    WEAK_PASSWORD("senha fraca"),
    BLANK_NAME("nome obrigatório"),
    CUSTOMER_NOT_FOUND("cliente não encontrado"),

    // Catalog – Category
    CATEGORY_NOT_FOUND("categoria não encontrada"),
    CATEGORY_HAS_PRODUCTS("categoria tem produtos vinculados"),
    INVALID_DISPLAY_ORDER("ordem inválida"),

    // Catalog – Product
    PRODUCT_NOT_FOUND("produto não encontrado"),
    INVALID_PRICE("preço inválido"),
    INVALID_STOCK("estoque inválido"),

    // Catalog – Operating Hours
    INVALID_HOURS("horário inválido"),

    // Delivery
    NEIGHBORHOOD_NOT_FOUND("bairro não encontrado"),
    NEIGHBORHOOD_ALREADY_EXISTS("bairro já cadastrado nessa cidade"),
    BLANK_CITY("cidade obrigatória"),
    INVALID_FEE("taxa inválida"),

    // Ordering
    CART_EMPTY("carrinho vazio"),
    CART_ITEM_NOT_FOUND("item do carrinho não encontrado"),
    INVALID_QUANTITY("quantidade inválida"),
    INVALID_OBSERVATION("observação inválida"),
    PRODUCT_UNAVAILABLE("produto indisponível"),
    PRODUCT_OUT_OF_STOCK("produto sem estoque suficiente"),
    VARIATION_NOT_FOUND("variação não encontrada"),
    ADDON_GROUP_NOT_FOUND("grupo de adicionais não encontrado"),
    ADDON_ITEM_NOT_FOUND("adicional não encontrado"),
    ADDON_GROUP_SELECTION_OUT_OF_RANGE("seleção de adicionais fora dos limites permitidos"),
    HALF_AND_HALF_NOT_ALLOWED("produto não permite meio-a-meio"),
    HALF_AND_HALF_REQUIRES_TWO_PRODUCTS("meio-a-meio requer dois produtos distintos"),
    HALF_AND_HALF_INCOMPATIBLE_WITH_VARIATION("meio-a-meio é incompatível com variação"),
    ORDER_NOT_FOUND("pedido não encontrado"),
    ORDER_INVALID_TRANSITION("transição de status inválida"),
    ORDER_ALREADY_TERMINAL("pedido já está em estado terminal"),
    ADDRESS_REQUIRED("endereço é obrigatório para entrega"),
    NEIGHBORHOOD_NOT_SERVED("bairro não atendido"),
    PICKUP_MUST_HAVE_NO_FEE("retirada não admite taxa de entrega"),
    IDEMPOTENCY_KEY_REQUIRED("Idempotency-Key obrigatório"),

    // Ordering – Dine-in (Table + Comanda)
    TABLE_NOT_FOUND("mesa não encontrada"),
    TABLE_NUMBER_TAKEN("número de mesa já cadastrado"),
    TABLE_INACTIVE("mesa inativa"),
    COMANDA_NOT_FOUND("comanda não encontrada"),
    COMANDA_ALREADY_OPEN("já existe uma comanda aberta nessa mesa"),
    COMANDA_CLOSED("comanda já está fechada"),
    COMANDA_NOT_MEMBER("cliente não faz parte da comanda"),
    COMANDA_HAS_PENDING_ORDERS("comanda tem pedidos pendentes"),
    DINE_IN_CONTEXT_REQUIRED("pedido DINE_IN requer tableId e comandaId"),

    // Promotion – Coupons
    COUPON_NOT_FOUND("cupom não encontrado"),
    COUPON_CODE_TAKEN("código de cupom já cadastrado"),
    INVALID_COUPON("cupom inválido"),
    COUPON_INACTIVE("cupom inativo"),
    COUPON_EXPIRED("cupom expirado"),
    COUPON_BELOW_MIN_ORDER("valor mínimo do pedido não atingido"),
    COUPON_EXHAUSTED("cupom esgotou os usos"),

    // Review
    ORDER_NOT_REVIEWABLE("pedido não pode ser avaliado"),
    REVIEW_ALREADY_SUBMITTED("avaliação já enviada para este pedido"),
    REVIEW_NOT_FOUND("avaliação não encontrada"),
    INVALID_RATING("rating inválido"),
    INVALID_COMMENT("comentário inválido"),

    // Payment
    PAYMENT_NOT_FOUND("pagamento não encontrado"),
    PAYMENT_ORDER_NOT_PAYABLE("pedido não está em estado pagável"),
    PAYMENT_ALREADY_INITIATED("já existe pagamento ativo para este pedido"),
    CARD_TOKEN_REQUIRED("cardToken obrigatório para pagamento por cartão"),
    PAYMENT_GATEWAY_FAILED("falha ao processar pagamento no gateway"),
    PAYMENT_INVALID_WEBHOOK_SIGNATURE("assinatura de webhook inválida"),
    PAYMENT_NOT_REFUNDABLE("pagamento não pode ser reembolsado neste estado");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
