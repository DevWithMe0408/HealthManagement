package org.example.nutritionservice.entity.catalog;

/**
 * Slot trong bua an — vi tri cua mon trong thuc don hang ngay.
 *
 * <ul>
 *   <li>CHINH:    Mon chinh (thit/ca/trung/dau) — protein nguon chinh</li>
 *   <li>RAU:      Mon rau (rau la xao/luoc, canh, rau cu)</li>
 *   <li>TINH_BOT: Tinh bot nen (com, bun, mi, banh mi) — an kem mon chinh</li>
 *   <li>COMBO:    Mon hoan chinh da chua ca 3 slot tren (pho, bun cha, com tam...)</li>
 *   <li>BUA_PHU:  Bua phu (trai cay, sua chua, hat, snack)</li>
 * </ul>
 */
public enum SlotCode {
    CHINH,
    RAU,
    TINH_BOT,
    COMBO,
    BUA_PHU
}
