package org.jasig.portlet.blackboardvcportlet.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.jasig.portlet.blackboardvcportlet.data.BasicUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapOperations;

@RunWith(MockitoJUnitRunner.class)
public class LdapUserServiceImplTest {
    @InjectMocks private LdapUserServiceImpl ldapUserServiceImpl;
    @Mock private LdapOperations ldapOperations;
    
    @Test
    public void testSearchByEmptyName() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByName("");
        assertEquals(0, result.size());
    }
    
    @Test
    public void testSearchByFirstName() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByName("John");
        assertEquals(0, result.size());
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(|(givenName=John*)(sn=John*)(cn=John*)))"), any(AttributesMapper.class));
    }
    
    @Test
    public void testSearchByFirstLastName() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByName("John Doe");
        assertEquals(0, result.size());
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(|(givenName=John*)(sn=Doe*)(cn=John*Doe*)))"), any(AttributesMapper.class));
    }
    
    @Test
    public void testSearchByFirstMiddleLastName() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByName("John C Doe");
        assertEquals(0, result.size());
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(|(givenName=John*)(sn=Doe*)(cn=John*C*Doe*)))"), any(AttributesMapper.class));
    }
    
    @Test
    public void testSearchByFirstMiddleMultiLastName() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByName("John C Doe Dum");
        assertEquals(0, result.size());
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(|(givenName=John*)(sn=Dum*)(cn=John*C*Doe*Dum*)))"), any(AttributesMapper.class));
    }
    
    @Test
    public void testSearchByEmail() {
        final Set<BasicUser> result = ldapUserServiceImpl.searchForUserByEmail("john.doe@example");
        assertEquals(0, result.size());
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(mail=john.doe@example*))"), any(AttributesMapper.class));
    }
    
    @Test
    public void testFindUser() {
        final BasicUser user = ldapUserServiceImpl.findUser("ID");
        assertNull(user);
        
        verify(ldapOperations).search(eq(""), eq("(&(objectclass=person)(uid=ID))"), any(AttributesMapper.class));
    }
}