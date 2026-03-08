package com.example.czConsv.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T2: サブシステム集計（バッチ集計結果） */
@Entity
@Table(name = "tcz13_subsys_sum")
public class Tcz13SubsysSum {

    @Id
    @Column(name = "yyyymm")
    public String yyyymm;

    @Column(name = "nendo_half")
    public String nendoHalf;

    @Column(name = "month")
    public String month;

    @Id
    @Column(name = "sumkbn")
    public String sumkbn;

    @Column(name = "sys_kbn")
    public String sysKbn;

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Id
    @Column(name = "subsknno")
    public String subsknno;

    @Column(name = "aplid")
    public String aplid;

    @Id
    @Column(name = "hs_syubetu")
    public String hsSyubetu;

    @Id
    @Column(name = "hs_unyou_kubun")
    public String hsUnyouKubun;

    @Id
    @Column(name = "hs_kategori_id")
    public String hsKategoriId;

    @Id
    @Column(name = "skbtcd")
    public String skbtcd;

    @Column(name = "hs_kousuu")
    public BigDecimal hsKousuu;

    @Column(name = "initnt")
    public String initnt;

    @Column(name = "inidate")
    public LocalDateTime inidate;

    @Column(name = "updtnt")
    public String updtnt;

    @Column(name = "upddate")
    public LocalDateTime upddate;

    @Column(name = "updpgid")
    public String updpgid;

    @Column(name = "delflg")
    public String delflg;
}
