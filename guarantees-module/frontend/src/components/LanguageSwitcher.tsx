import React from 'react';
import { Select, Space } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Option } = Select;

interface LanguageSwitcherProps {
  placement?: 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight';
  size?: 'small' | 'middle' | 'large';
  showText?: boolean;
}

const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({
  placement = 'bottomLeft',
  size = 'middle',
  showText = false
}) => {
  const { i18n, t } = useTranslation();

  const handleLanguageChange = (language: string) => {
    i18n.changeLanguage(language);
  };

  const currentLanguage = i18n.language || 'en';

  return (
    <Space size="small">
      {showText && (
        <span style={{ color: '#666' }}>
          <GlobalOutlined style={{ marginRight: 4 }} />
          {t('settings.language')}:
        </span>
      )}
      <Select
        value={currentLanguage}
        onChange={handleLanguageChange}
        size={size}
        style={{ width: 120 }}
        placement={placement}
        suffixIcon={!showText ? <GlobalOutlined /> : undefined}
      >
        <Option value="en">
          <Space>
            <span role="img" aria-label="English">🇺🇸</span>
            {t('settings.languages.en')}
          </Space>
        </Option>
        <Option value="es">
          <Space>
            <span role="img" aria-label="Spanish">🇪🇸</span>
            {t('settings.languages.es')}
          </Space>
        </Option>
        <Option value="de">
          <Space>
            <span role="img" aria-label="German">🇩🇪</span>
            {t('settings.languages.de')}
          </Space>
        </Option>
      </Select>
    </Space>
  );
};

export default LanguageSwitcher;
