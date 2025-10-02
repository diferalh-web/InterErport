import React, { useState } from 'react';
import { Layout as AntLayout, Menu, Typography, theme, Breadcrumb, Avatar, Dropdown } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  DashboardOutlined,
  FileTextOutlined,
  PlusOutlined,
  DollarCircleOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  BarChartOutlined
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import LanguageSwitcher from './LanguageSwitcher';

const { Header, Sider, Content } = AntLayout;
const { Title, Text } = Typography;

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const {
    token: { colorBgContainer },
  } = theme.useToken();

  const menuItems: MenuProps['items'] = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: t('navigation.dashboard'),
    },
    {
      key: '/guarantees',
      icon: <FileTextOutlined />,
      label: t('navigation.guarantees'),
      children: [
        {
          key: '/guarantees',
          label: t('common.view') + ' ' + t('navigation.guarantees'),
        },
        {
          key: '/guarantees/new',
          icon: <PlusOutlined />,
          label: t('guarantees.createNew'),
        },
      ],
    },
    {
      key: '/fx-rates',
      icon: <DollarCircleOutlined />,
      label: 'FX Rates',
    },
    {
      key: '/clients',
      icon: <UserOutlined />,
      label: t('navigation.clients'),
    },
    {
      key: '/reports',
      icon: <BarChartOutlined />,
      label: t('navigation.reports'),
    },
  ];

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: t('navigation.profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: t('navigation.settings'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: t('navigation.logout'),
      danger: true,
    },
  ];

  const handleMenuClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  const handleUserMenuClick: MenuProps['onClick'] = (e) => {
    if (e.key === 'logout') {
      localStorage.removeItem('auth_token');
      navigate('/login');
    }
  };

  const getBreadcrumbItems = () => {
    const pathSegments = location.pathname.split('/').filter(Boolean);
    const breadcrumbItems = [
      {
        title: t('navigation.dashboard'),
        href: '/dashboard',
      },
    ];

    let currentPath = '';
    pathSegments.forEach((segment, index) => {
      currentPath += `/${segment}`;
      
      let title = '';
      
      // Custom titles for specific routes using translations
      if (segment === 'guarantees' && pathSegments[index + 1] === 'new') {
        return; // Skip, will be handled by 'new'
      }
      
      if (segment === 'new') {
        title = t('guarantees.createNew');
      } else if (segment === 'guarantees') {
        title = t('navigation.guarantees');
      } else if (segment === 'fx-rates') {
        title = 'FX Rates';
      } else if (segment === 'clients') {
        title = t('navigation.clients');
      } else if (segment === 'reports') {
        title = t('navigation.reports');
      } else if (segment === 'claims') {
        title = t('navigation.claims');
      } else if (segment === 'amendments') {
        title = t('navigation.amendments');
      } else if (segment.match(/^\d+$/)) {
        title = `${t('guarantees.title')} #${segment}`;
      } else {
        title = segment.charAt(0).toUpperCase() + segment.slice(1);
      }

      breadcrumbItems.push({
        title,
        href: currentPath,
      });
    });

    return breadcrumbItems;
  };

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        style={{
          background: colorBgContainer,
          borderRight: '1px solid #f0f0f0',
        }}
      >
        <div style={{ 
          padding: '16px', 
          textAlign: 'center', 
          borderBottom: '1px solid #f0f0f0',
          background: '#001529'
        }}>
          {!collapsed ? (
            <Title level={4} style={{ color: 'white', margin: 0 }}>
              {t('navigation.guarantees')} Module
            </Title>
          ) : (
            <Title level={4} style={{ color: 'white', margin: 0 }}>
              GM
            </Title>
          )}
        </div>
        
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ height: '100%', borderRight: 0 }}
        />
      </Sider>
      
      <AntLayout>
        <Header style={{ 
          padding: '0 24px', 
          background: colorBgContainer,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #f0f0f0',
        }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
              className: 'trigger',
              onClick: () => setCollapsed(!collapsed),
              style: { fontSize: '18px', cursor: 'pointer' },
            })}
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <LanguageSwitcher size="small" />
            <BellOutlined style={{ fontSize: '18px', cursor: 'pointer' }} />
            
            <Dropdown
              menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
              placement="bottomRight"
            >
              <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: '8px' }}>
                <Avatar icon={<UserOutlined />} />
                <Text strong>Admin User</Text>
              </div>
            </Dropdown>
          </div>
        </Header>
        
        <div style={{ padding: '16px 24px 0' }}>
          <Breadcrumb items={getBreadcrumbItems()} />
        </div>
        
        <Content
          style={{
            margin: '16px 24px',
            padding: '24px',
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: '8px',
          }}
        >
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
