package ph.com.guanzongroup.cas.purchasing.ui;

import org.guanzon.appdriver.base.GRiderCAS;

/**
 * What a screen receives before it is shown (same as integsys'
 * ph.com.guanzongroup.integsys.views.ScreenInterface).
 */
public interface ScreenInterface {

    void setGRider(GRiderCAS foValue);

    void setIndustryID(String fsValue);

    void setCompanyID(String fsValue);

    void setCategoryID(String fsValue);
}
