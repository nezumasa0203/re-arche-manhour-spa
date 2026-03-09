package com.example.czConsv.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T1: 保守工数（メインテーブル） */
@Entity
@Table(name = "tcz01_hosyu_kousuu")
public class Tcz01HosyuKousuu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seqno")
    public Long seqNo;

    @Column(name = "skbtcd")
    public String skbtcd;

    @Column(name = "hssgytnt_esqid")
    public String hssgytntEsqid;

    @Column(name = "hssgytnt_name")
    public String hssgytntName;

    @Column(name = "year_half")
    public String yearHalf;

    @Column(name = "sgyymd")
    public LocalDate sgyymd;

    @Column(name = "sys_kbn")
    public String sysKbn;

    @Column(name = "cause_sys_kbn")
    public String causeSysKbn;

    @Column(name = "taisyo_sknno")
    public String taisyoSknno;

    @Column(name = "taisyo_subsysno")
    public String taisyoSubsysno;

    @Column(name = "taisyo_aplid")
    public String taisyoAplid;

    @Column(name = "cause_sysno")
    public String causeSysno;

    @Column(name = "cause_subsysno")
    public String causeSubsysno;

    @Column(name = "cause_aplid")
    public String causeAplid;

    @Column(name = "kenmei")
    public String kenmei;

    @Column(name = "hs_kategori")
    public String hsKategori;

    @Column(name = "hs_syubetu")
    public String hsSyubetu;

    @Column(name = "hs_unyou_kubun")
    public String hsUnyouKubun;

    @Column(name = "tmr_no")
    public String tmrNo;

    @Column(name = "sgy_iraisyo_no")
    public String sgyIraisyoNo;

    @Column(name = "sgy_iraisya_esqid")
    public String sgyIraisyaEsqid;

    @Column(name = "sgy_iraisya_name")
    public String sgyIraisyaName;

    @Column(name = "status")
    public String status;

    @Column(name = "kousuu")
    public BigDecimal kousuu;

    @Column(name = "initnt")
    public String initnt;

    @Column(name = "inidate")
    public LocalDateTime inidate;

    @Column(name = "updtnt")
    public String updtnt;

    /** 楽観ロック用（CZ-101） */
    @Column(name = "upddate")
    public LocalDateTime upddate;

    @Column(name = "updpgid")
    public String updpgid;

    @Column(name = "delflg")
    public String delflg;
}
