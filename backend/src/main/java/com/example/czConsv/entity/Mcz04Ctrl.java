package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M2: コントロールマスタ（月次処理制御） */
@Entity
@Table(name = "mcz04_ctrl")
public class Mcz04Ctrl {

    /** '00'=管理モード(jinjiMode:false), '01'=人事モード(jinjiMode:true) */
    @Id
    @Column(name = "sysid")
    public String sysid;

    @Id
    @Column(name = "yyyymm")
    public String yyyymm;

    @Column(name = "online_flg")
    public String onlineFlg;

    @Column(name = "renketsu_flg")
    public String renketsuFlg;

    @Column(name = "gjkt_flg")
    public String gjktFlg;

    @Column(name = "data_sk_flg")
    public String dataSkFlg;

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
