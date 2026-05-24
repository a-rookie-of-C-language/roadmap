package com.roadmap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mc", nullable = false)
    private String mc;

    @Column(name = "typeid", nullable = false)
    private String typeid;

    @Column(name = "admin_area_name")
    private String adminAreaName;

    @Column(name = "szwz")
    private String szwz;

    @Column(name = "gldw")
    private String gldw;

    @Column(name = "gldwname")
    private String gldwname;

    @Column(name = "yhdw")
    private String yhdw;

    @Column(name = "yhdwname")
    private String yhdwname;

    @Column(name = "zt")
    private String zt;

    @Column(name = "radius_meters")
    private Double radiusMeters;

    @Column(name = "geom", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (radiusMeters == null) {
            radiusMeters = 100D;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMc() {
        return mc;
    }

    public void setMc(String mc) {
        this.mc = mc;
    }

    public String getTypeid() {
        return typeid;
    }

    public void setTypeid(String typeid) {
        this.typeid = typeid;
    }

    public String getAdminAreaName() {
        return adminAreaName;
    }

    public void setAdminAreaName(String adminAreaName) {
        this.adminAreaName = adminAreaName;
    }

    public String getSzwz() {
        return szwz;
    }

    public void setSzwz(String szwz) {
        this.szwz = szwz;
    }

    public String getGldw() {
        return gldw;
    }

    public void setGldw(String gldw) {
        this.gldw = gldw;
    }

    public String getGldwname() {
        return gldwname;
    }

    public void setGldwname(String gldwname) {
        this.gldwname = gldwname;
    }

    public String getYhdw() {
        return yhdw;
    }

    public void setYhdw(String yhdw) {
        this.yhdw = yhdw;
    }

    public String getYhdwname() {
        return yhdwname;
    }

    public void setYhdwname(String yhdwname) {
        this.yhdwname = yhdwname;
    }

    public String getZt() {
        return zt;
    }

    public void setZt(String zt) {
        this.zt = zt;
    }

    public Double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public Point getGeom() {
        return geom;
    }

    public void setGeom(Point geom) {
        this.geom = geom;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
