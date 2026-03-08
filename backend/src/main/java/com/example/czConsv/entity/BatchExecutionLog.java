package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T6: バッチ実行履歴（監視・障害調査用） */
@Entity
@Table(name = "batch_execution_log")
public class BatchExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "batch_name")
    public String batchName;

    @Column(name = "started_at")
    public LocalDateTime startedAt;

    @Column(name = "finished_at")
    public LocalDateTime finishedAt;

    @Column(name = "status")
    public String status;

    @Column(name = "records_affected")
    public Integer recordsAffected;

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "created_at")
    public LocalDateTime createdAt;
}
