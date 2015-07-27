package io.pivotal.portfolio.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author sgupta
 * @since 7/23/15.
 */
public class PortfolioValue {
  private String accountId;
  private String name;
  private BigDecimal currentTotalValue = BigDecimal.ZERO;
  private BigDecimal purchaseValue = BigDecimal.ZERO;
  private BigDecimal sellValue = BigDecimal.ZERO;
  private BigDecimal change = BigDecimal.ZERO;
  private BigDecimal pctChange = BigDecimal.ZERO;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getCurrentTotalValue() {
    return currentTotalValue;
  }

  public void setCurrentTotalValue(BigDecimal currentTotalValue) {
    this.currentTotalValue = currentTotalValue;
  }

  public BigDecimal getPurchaseValue() {
    return purchaseValue;
  }

  public void setPurchaseValue(BigDecimal purchaseValue) {
    this.purchaseValue = purchaseValue;
  }

  public BigDecimal getSellValue() {
    return sellValue;
  }

  public void setSellValue(BigDecimal sellValue) {
    this.sellValue = sellValue;
  }

  public BigDecimal getChange() {
    return change;
  }

  public void setChange(BigDecimal change) {
    this.change = change;
  }

  public BigDecimal getPctChange() {
    return pctChange;
  }

  public void setPctChange(BigDecimal pctChange) {
    this.pctChange = pctChange;
  }

  public void derive() {
    if(!Objects.equals(currentTotalValue, BigDecimal.ZERO) && !Objects.equals(purchaseValue, BigDecimal.ZERO)) {
      change = currentTotalValue.subtract(purchaseValue);
      pctChange = change.divide(purchaseValue, BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100L));
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PortfolioValue{");
    sb.append("accountId='").append(accountId).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append(", currentTotalValue=").append(currentTotalValue);
    sb.append(", purchaseValue=").append(purchaseValue);
    sb.append(", sellValue=").append(sellValue);
    sb.append(", change=").append(change);
    sb.append(", pctChange=").append(pctChange);
    sb.append('}');
    return sb.toString();
  }
}
