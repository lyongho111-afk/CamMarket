package com.campus.remarket.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Preserve old notifications while allowing one notification per trade participant. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotificationRecipientMigration implements ApplicationRunner {
    private final JdbcTemplate db;
    public NotificationRecipientMigration(JdbcTemplate db){this.db=db;}
    @Override public void run(ApplicationArguments args){migrate();}
    public void migrate(){
        // Install the stronger per-recipient protection before removing the old
        // one-recipient-per-order constraint. No rows or delivery states change.
        db.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_recipient_idx ON seller_notifications(order_id,seller_member_id)");
        List<String> legacy=db.queryForList("""
            SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
            JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
              ON tc.CONSTRAINT_CATALOG=k.CONSTRAINT_CATALOG AND tc.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA
              AND tc.CONSTRAINT_NAME=k.CONSTRAINT_NAME
            WHERE tc.TABLE_SCHEMA='PUBLIC' AND tc.TABLE_NAME='SELLER_NOTIFICATIONS'
              AND tc.CONSTRAINT_TYPE='UNIQUE'
            GROUP BY tc.CONSTRAINT_NAME
            HAVING COUNT(*)=1 AND MAX(k.COLUMN_NAME)='ORDER_ID'
            """,String.class);
        for(String name:legacy){
            if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected notification constraint name");
            db.execute("ALTER TABLE seller_notifications DROP CONSTRAINT \""+name+"\"");
        }
    }
}
