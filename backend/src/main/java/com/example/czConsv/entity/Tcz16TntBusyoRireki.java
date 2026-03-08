package com.example.czConsv.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T5: 担当部署履歴（人事異動） */
@Entity
@Table(name = "tcz16_tnt_busyo_rireki")
public class Tcz16TntBusyoRireki {

    @Id
    @Column(name = "tnt_kubun")
    public String tntKubun;

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Column(name = "tnt_str_ymd")
    public LocalDate tntStrYmd;

    @Id
    @Column(name = "tnt_end_ymd")
    public LocalDate tntEndYmd;

    @Column(name = "tnt_busyo")
    public String tntBusyo;

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
