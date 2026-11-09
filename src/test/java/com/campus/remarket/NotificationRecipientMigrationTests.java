package com.campus.remarket;

import com.campus.remarket.config.NotificationRecipientMigration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class NotificationRecipientMigrationTests {
    @Test void existingSellerNotificationAndReceiptSurviveRepeatMigration(){
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:notificationmigration;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE seller_notifications(notification_id VARCHAR(36) PRIMARY KEY,order_id VARCHAR(64) NOT NULL UNIQUE,seller_member_id BIGINT NOT NULL,read_at TIMESTAMP)");
        db.execute("CREATE TABLE notification_deliveries(notification_id VARCHAR(36) REFERENCES seller_notifications(notification_id),status VARCHAR(30))");
        db.update("INSERT INTO seller_notifications VALUES('seller','trade',1,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO notification_deliveries VALUES('seller','ACCEPTED')");
        var migration=new NotificationRecipientMigration(db);migration.migrate();migration.migrate();
        db.update("INSERT INTO seller_notifications VALUES('buyer','trade',2,NULL)");
        assertThrows(DuplicateKeyException.class,()->db.update("INSERT INTO seller_notifications VALUES('duplicate','trade',2,NULL)"));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM seller_notifications",Integer.class));
        assertNotNull(db.queryForObject("SELECT read_at FROM seller_notifications WHERE notification_id='seller'",java.sql.Timestamp.class));
        assertEquals("ACCEPTED",db.queryForObject("SELECT status FROM notification_deliveries WHERE notification_id='seller'",String.class));
    }
}
