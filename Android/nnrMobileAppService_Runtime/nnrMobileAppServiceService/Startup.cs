using Microsoft.Owin;
using Owin;

[assembly: OwinStartup(typeof(nnrMobileAppServiceService.Startup))]

namespace nnrMobileAppServiceService
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureMobileApp(app);
        }
    }
}