package org.example.nutritionservice.entity.catalog;

/**
 * Nhom thuc pham chinh — dung cho ca dish va ingredient.
 *
 * <p>Dung cho dish.food_group_code (12 gia tri bao gom COMBO/BUA_PHU)
 * <br>Dung cho ingredient.group_code (11 gia tri, KHONG co COMBO, co them GIA_VI)
 *
 * <p>Luu y: GIA_VI chi ap dung cho ingredient (gia vi, dau, sot...),
 * dish khong co food_group = GIA_VI.
 */
public enum FoodGroup {
    // Protein
    GIA_CAM,        // Ga, vit, chim cut
    THIT_DO,        // Heo, bo
    CA,             // Ca nuoc ngot/man (chi dish, ingredient dung HAI_SAN)
    HAI_SAN,        // Tom, muc, cua, oc, ngao + ca (cho ingredient)
    TRUNG,          // Trung ga, vit, cut
    DAU_DO,         // Dau phu, dau cac loai

    // Rau
    RAU_LA,         // Rau la
    RAU_CU,         // Rau cu

    // Tinh bot
    TINH_BOT_GAO,   // Gao, com, xoi, chao
    TINH_BOT_MI,    // Bun, pho, mi, banh mi

    // Loai dac biet
    COMBO,          // Chi dung cho dish (pho, bun cha...), KHONG dung cho ingredient
    BUA_PHU,        // Trai cay, sua, hat, snack
    GIA_VI          // Chi dung cho ingredient (gia vi, sot, dau, nuoc dung)
}
