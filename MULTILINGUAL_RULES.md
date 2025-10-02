# InterErport Guarantees Module - Multilingual Implementation Rules & Best Practices

## Version: 1.0
## Created: October 2025
## Author: AI Assistant & Development Team

---

## 🌍 **Core Principles**

### 1. **Translation-First Development**
- All UI text MUST be externalized from components
- No hardcoded strings in React components
- Use translation keys instead of English text directly
- Provide meaningful fallback values using `defaultValue` parameter

### 2. **Consistent Translation Key Structure**
```
namespace.subnamespace.key
Examples:
- common.buttons.save  
- guarantees.statuses.DRAFT
- validation.required
- forms.placeholders.enterText
```

### 3. **Mandatory Translation Hook Usage**
```typescript
// ✅ CORRECT - Use enhanced translation hook
import { useAppTranslation } from '../i18n/utils';
const { t, formatCurrency, formatDate } = useAppTranslation();

// ❌ WRONG - Don't use basic useTranslation directly
import { useTranslation } from 'react-i18next';
const { t } = useTranslation();
```

---

## 🔧 **Implementation Standards**

### 4. **Button Functionality Preservation**
**CRITICAL RULE**: Translation changes must NEVER break button functionality

#### ✅ **Correct Implementation**
```typescript
// Good - Function reference preserved
<Button onClick={handleSubmit}>
  {t('buttons.submit')}
</Button>

// Good - Props properly maintained
<Button 
  type="primary"
  loading={isLoading}
  onClick={() => handleAction(id)}
>
  {t('buttons.approve')}
</Button>
```

#### ❌ **Common Mistakes to Avoid**
```typescript
// Wrong - Don't change function signatures when adding translations
<Button onClick={t('buttons.submit')}>  // ❌ This breaks functionality!

// Wrong - Don't accidentally modify component props during translation
<Button type={t('buttons.primary')} onClick={handleSubmit}>  // ❌ Type should stay "primary"
```

### 5. **Form Validation Messages**
```typescript
// ✅ Correct validation with translations
rules={[
  { required: true, message: t('validation.required') },
  { max: 140, message: t('validation.maxLength', { max: 140 }) }
]}

// Use interpolation for dynamic values
message: t('validation.min', { min: 0.01 })
```

### 6. **Table and Data Display**
```typescript
// ✅ Table headers
title: t('table.columns.reference')

// ✅ Status rendering with translations
render: (status) => (
  <Tag color={statusColors[status]}>
    {t(`guarantees.statuses.${status}`)}
  </Tag>
)

// ✅ Pagination with translations
showTotal: (total, range) => t('table.showingItems', { 
  start: range[0], end: range[1], total 
})
```

---

## 📁 **File Structure Standards**

### 7. **Translation File Organization**
```
src/i18n/
├── index.ts          # Main i18n configuration
├── config.ts         # Language definitions & cultural settings
├── utils.ts          # Translation utilities & hooks
└── locales/
    ├── en.json       # English translations
    ├── es.json       # Spanish translations
    ├── de.json       # German translations
    └── ...           # Additional languages
```

### 8. **Translation Key Hierarchies**
```json
{
  "common": { "loading", "error", "success", "buttons": {} },
  "navigation": { "dashboard", "guarantees", ... },
  "guarantees": { 
    "title", "statuses": {}, "guaranteeTypes": {}, ...
  },
  "validation": { "required", "email", "maxLength", ... },
  "forms": { "placeholders": {}, "basicInformation", ... },
  "tables": { "columns": {}, "noData", "loading", ... },
  "messages": { "saveSuccess", "loadError", ... }
}
```

---

## 🛠️ **Technical Requirements**

### 9. **Import Management**
```typescript
// ✅ Always clean up unused imports when adding translations
import { useAppTranslation } from '../i18n/utils';

// Remove old unused imports like:
// ❌ import { GuaranteeStatusLabels, GuaranteeTypeLabels } from '../types/guarantee';
```

### 10. **TypeScript Compliance**
- All translation implementations must be TypeScript compliant
- Use proper typing for translation function calls
- Handle optional/undefined values gracefully
- Provide fallback values for robust error handling

### 11. **Currency and Date Formatting**
```typescript
// ✅ Use built-in formatting utilities
const { formatCurrency, formatDate } = useAppTranslation();

// Currency with proper locale
{formatCurrency(amount, currency)}

// Date with cultural preferences  
{formatDate(date)}
```

---

## 🧪 **Testing Standards**

### 12. **Mandatory Testing Checklist**
Before completing any multilingual feature:

- [ ] Test functionality in ALL supported languages (EN/ES/DE)
- [ ] Verify buttons work correctly after language switch
- [ ] Confirm form submissions function in all languages
- [ ] Check modal dialogs and notifications display properly
- [ ] Validate table sorting/filtering works in all languages
- [ ] Test API calls are not affected by language changes

### 13. **Language Switching Validation**
```typescript
// Test this workflow for EVERY component:
1. Load component in English → Verify all functions work
2. Switch to Spanish → Test same functions
3. Switch to German → Test same functions  
4. Switch back to English → Retest functions
```

---

## 🚨 **Error Prevention Rules**

### 14. **Component State Management**
```typescript
// ✅ Good - Translation doesn't affect component state
const [isLoading, setIsLoading] = useState(false);
const { t } = useAppTranslation();

return (
  <Button loading={isLoading} onClick={handleSubmit}>
    {t('buttons.submit')}
  </Button>
);

// ❌ Bad - Don't let translations interfere with state
const { t } = useAppTranslation();
const [buttonText, setButtonText] = useState(t('buttons.submit')); // Wrong approach!
```

### 15. **API Integration**
```typescript
// ✅ API calls should be language-agnostic
try {
  await apiService.createGuarantee(data);
  message.success(t('messages.createSuccess'));
} catch (error) {
  message.error(t('messages.createError'));
}

// ❌ Don't send translated text to backend
// API payload should contain raw data, not translated strings
```

---

## 📋 **Component Update Checklist**

### 16. **Standard Component Translation Process**
1. **Import Phase**
   - [ ] Add `useAppTranslation` import
   - [ ] Remove unused label/enum imports
   - [ ] Clean up TypeScript warnings

2. **Hook Integration**
   - [ ] Add `const { t, formatCurrency, formatDate } = useAppTranslation();`
   - [ ] Verify hook placement inside component function

3. **UI Element Updates**
   - [ ] Replace hardcoded titles with `t('namespace.key')`
   - [ ] Update button labels with `t('buttons.action')`
   - [ ] Translate form labels and placeholders
   - [ ] Convert table headers to translations
   - [ ] Update validation messages

4. **State & Logic Preservation**
   - [ ] Verify all onClick handlers remain unchanged
   - [ ] Confirm form submission logic is unaffected
   - [ ] Test API calls still function correctly
   - [ ] Validate state management is preserved

5. **Testing & Validation**
   - [ ] Test component in all languages
   - [ ] Verify functionality works after language switches
   - [ ] Check for TypeScript errors
   - [ ] Validate performance is not impacted

---

## 🎯 **Quality Assurance**

### 17. **Code Review Standards**
When reviewing multilingual PRs, verify:
- No hardcoded strings remain in components
- All button functionality is preserved
- Translation keys follow naming conventions
- Fallback values are provided
- TypeScript errors are resolved
- Testing checklist is completed

### 18. **Performance Considerations**
- Translations should be loaded lazily when possible
- Avoid re-rendering components unnecessarily on language change
- Cache formatted currencies and dates appropriately
- Minimize translation file size by avoiding redundancy

---

## 🔄 **Maintenance & Updates**

### 19. **Adding New Languages**
1. Create new locale file (`/locales/xx.json`)
2. Update `SUPPORTED_LANGUAGES` in `config.ts`
3. Add language option to `LanguageSwitcher`
4. Import required antd and dayjs locales
5. Test thoroughly with new language

### 20. **Future Module Development**
When creating new modules:
- Start with translation structure planning
- Use existing translation namespaces where possible
- Follow established key naming patterns
- Implement multilingual support from day one
- Reference this document for consistency

---

## 📖 **Reference Examples**

### Complete Component Translation Example:
```typescript
import { useAppTranslation } from '../i18n/utils';

const MyComponent: React.FC = () => {
  const { t, formatCurrency } = useAppTranslation();

  return (
    <Card title={t('mymodule.title')}>
      <Form>
        <Form.Item
          label={t('mymodule.amount')}
          rules={[{ required: true, message: t('validation.required') }]}
        >
          <InputNumber placeholder={t('forms.placeholders.enterAmount')} />
        </Form.Item>
        <Button type="primary" onClick={handleSubmit}>
          {t('buttons.submit')}
        </Button>
      </Form>
    </Card>
  );
};
```

---

**Remember: These rules exist to ensure consistent, maintainable, and robust multilingual implementations. Always test functionality across all languages before considering any translation work complete.**




