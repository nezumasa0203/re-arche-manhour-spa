package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T4: MYシステム（お気に入り） */
@Entity
@Table(name = "tcz19_my_sys")
public class Tcz19MySys {

    @Id
    @Column(name = "tnt_esqid")
    public String tntEsqid;

    @Id
    @Column(name = "sknno")
    public String sknno;

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
